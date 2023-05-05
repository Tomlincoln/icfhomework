package hu.tomlincoln.icfhomework.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import hu.tomlincoln.icfhomework.dto.ArticArtworkListResponse;
import hu.tomlincoln.icfhomework.dto.ArticArtworkResponse;
import hu.tomlincoln.icfhomework.dto.ArtworkResponse;
import hu.tomlincoln.icfhomework.entity.Artwork;
import hu.tomlincoln.icfhomework.entity.User;
import hu.tomlincoln.icfhomework.exception.ArtworkNotFoundException;
import hu.tomlincoln.icfhomework.repository.ArtworkRepository;
import hu.tomlincoln.icfhomework.repository.UserRepository;

@Service
public class ArtworkService {

    public static final String FIELD_SELECTOR = "fields=id,title,artist_title,thumbnail";
    public static final String USER_AGENT_HEADER = "AIC-User-Agent";
    public static final String USER_AGENT_HEADER_VALUE = "ICFHomework (tomlincoln93@gmail.com)";
    public static final String ARTIC_API = "https://api.artic.edu/api/v1/";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtworkService.class);

    private final ArtworkRepository artworkRepository;
    private final UserRepository userRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public ArtworkService(ArtworkRepository artworkRepository, UserRepository userRepository) {
        this(artworkRepository, userRepository, HttpClient.newHttpClient(), new ObjectMapper());
    }

    // Visibility for testing
    protected ArtworkService(ArtworkRepository artworkRepository, UserRepository userRepository, HttpClient client, ObjectMapper objectMapper) {
        this.artworkRepository = artworkRepository;
        this.userRepository = userRepository;
        this.httpClient = client;
        this.objectMapper = objectMapper;
    }

    public ArtworkResponse getArtwork(Integer id) {
        ArtworkResponse artworkResponse = new ArtworkResponse();
        Artwork artwork = artworkRepository.findById(id).orElseGet(() -> lookupAndSave(id).orElseThrow(ArtworkNotFoundException::new));
        artworkResponse.setId(artwork.getId());
        artworkResponse.setTitle(artwork.getTitle());
        artworkResponse.setAuthor(artwork.getAuthor());
        if (artwork.getThumbnail() != null) {
            artworkResponse.setThumbnail(artwork.getThumbnail());
        }
        artworkResponse.setPurchasedBy(artwork.getPurchasedBy() != null ? artwork.getPurchasedBy().getId() : null);
        return artworkResponse;
    }

    public void purchaseArtwork(Integer id, Integer userId) {
        Artwork artwork = artworkRepository.findById(id).orElseGet(() -> lookupAndSave(id).orElseThrow(ArtworkNotFoundException::new));
        if (artwork.getPurchasedBy() == null) {
            User user = userRepository.getReferenceById(userId);
            user.getArtworks().add(artwork);
            userRepository.save(user);
        }
    }

    private Optional<Artwork> lookupAndSave(Integer id) {
        Optional<Artwork> toReturn = Optional.empty();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ARTIC_API + "artworks/" + id + "?" + FIELD_SELECTOR))
                .headers(USER_AGENT_HEADER, USER_AGENT_HEADER_VALUE)
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response != null && response.statusCode() == HttpStatus.OK.value()) {
                Artwork artwork = new Artwork();
                String pureResponseBody = response.body().substring(8, response.body().length() - 1);
                ArticArtworkResponse artworkResponse = objectMapper.readValue(pureResponseBody, ArticArtworkResponse.class);
                artwork.setId(artworkResponse.getId());
                artwork.setTitle(artworkResponse.getTitle());
                artwork.setAuthor(artworkResponse.getArtist());
                artwork.setThumbnail(artworkResponse.getThumbnail().getLqip());
                toReturn = Optional.of(artworkRepository.save(artwork));
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        return toReturn;
    }

    public List<ArtworkResponse> getArtworksPaginated(Integer pageNum, Integer pageSize) {
        List<ArtworkResponse> artworks = new ArrayList<>();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ARTIC_API + "artworks/" + "?" + FIELD_SELECTOR + "&page=" + pageNum + "&limit=" + pageSize))
                .headers(USER_AGENT_HEADER, USER_AGENT_HEADER_VALUE)
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response != null && response.statusCode() == HttpStatus.OK.value()) {
                ArticArtworkListResponse articArtworkListResponse = objectMapper.readValue(response.body(), ArticArtworkListResponse.class);
                for (ArticArtworkResponse articArtworkResponse : articArtworkListResponse.getData())
                {
                    ArtworkResponse artworkResponse = new ArtworkResponse();
                    artworkResponse.setId(articArtworkResponse.getId());
                    artworkResponse.setTitle(articArtworkResponse.getTitle());
                    artworkResponse.setAuthor(articArtworkResponse.getArtist());
                    if (articArtworkResponse.getThumbnail() != null && articArtworkResponse.getThumbnail().getLqip() != null) {
                        artworkResponse.setThumbnail(articArtworkResponse.getThumbnail().getLqip());
                    }
                    artworks.add(artworkResponse);
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        return artworks;
    }
}
