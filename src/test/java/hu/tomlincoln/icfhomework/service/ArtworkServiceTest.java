package hu.tomlincoln.icfhomework.service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import hu.tomlincoln.icfhomework.dto.ArticArtworkListResponse;
import hu.tomlincoln.icfhomework.dto.ArticArtworkResponse;
import hu.tomlincoln.icfhomework.dto.ArticThumbnailResponse;
import hu.tomlincoln.icfhomework.dto.ArtworkResponse;
import hu.tomlincoln.icfhomework.entity.Artwork;
import hu.tomlincoln.icfhomework.entity.User;
import hu.tomlincoln.icfhomework.exception.ArtworkNotFoundException;
import hu.tomlincoln.icfhomework.repository.ArtworkRepository;
import hu.tomlincoln.icfhomework.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import static hu.tomlincoln.icfhomework.service.ArtworkService.ARTIC_API;
import static hu.tomlincoln.icfhomework.service.ArtworkService.FIELD_SELECTOR;
import static hu.tomlincoln.icfhomework.service.ArtworkService.USER_AGENT_HEADER;
import static hu.tomlincoln.icfhomework.service.ArtworkService.USER_AGENT_HEADER_VALUE;

class ArtworkServiceTest {

    private static final Integer TEST_ID = 1;
    private static final Integer SECOND_TEST_ID = 2;
    private static final Integer TEST_USER_ID = 1;
    private static final String TEST_AUTHOR = "TestAuthor";
    private static final String TEST_TITLE = "Test Title";
    private static final String TEST_THUMBNAIL_DATA = "9asd7869as6d9as79d87as897d";
    private static final String ARTIC_MOCK_JSON = "{\"data\":{\"id\":\"1\",\"title\":\"\",\"artist_title\":\"\",\"thumbnail\":\"\"}}";
    private static final String ARTIC_LIST_ONE_ELEMENT_MOCK_JSON = "{\"pagination\":null,\"data\":" +
            "[{\"id\":\"1\",\"title\":\"\",\"artist_title\":\"\",\"thumbnail\":\"\"}],\"info\":null,\"config\":null}";
    private static final String ARTIC_LIST_TWO_ELEMENT_MOCK_JSON = "{\"pagination\":null,\"data\":" +
            "[{\"id\":\"1\",\"title\":\"\",\"artist_title\":\"\",\"thumbnail\":\"\"}," +
            "{\"id\":\"2\",\"title\":\"\",\"artist_title\":\"\",\"thumbnail\":\"\"}],\"info\":null,\"config\":null}";
    private static final Integer PAGE_NUMBER = 1;
    private static final Integer PAGE_SIZE = 10;

    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HttpClient client;
    @Mock
    private ObjectMapper objectMapper;
    @Captor
    private ArgumentCaptor<HttpRequest> httpRequestArgumentCaptor;
    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getArtwork_shouldReturnArtworkWhenFoundInDatabase() {
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.of(getDefaultArtwork()));

        ArtworkService service = new ArtworkService(artworkRepository, userRepository);
        ArtworkResponse response = service.getArtwork(TEST_ID);

        assertValidResponse(response);
        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
    }

    @Test
    public void getArtwork_shouldCallToArticChicagoAndReturnThatArtworkWhenNotInDatabase() throws IOException, InterruptedException {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.OK.value(), ARTIC_MOCK_JSON);

        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        Mockito.when(client.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);
        Mockito.when(objectMapper.readValue(anyString(), eq(ArticArtworkResponse.class))).thenReturn(getArticArtworkResponse());
        Mockito.when(artworkRepository.save(any(Artwork.class))).thenReturn(getDefaultArtwork());

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        ArtworkResponse response = service.getArtwork(TEST_ID);

        HttpRequest request = httpRequestArgumentCaptor.getValue();
        assertEquals(ARTIC_API + "artworks/" + TEST_ID + "?" + FIELD_SELECTOR, request.uri().toString());
        assertTrue(request.headers().map().containsKey(USER_AGENT_HEADER));
        assertEquals(1, request.headers().map().get(USER_AGENT_HEADER).size());
        assertEquals(USER_AGENT_HEADER_VALUE, request.headers().firstValue(USER_AGENT_HEADER).get());
        assertValidResponse(response);
        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
        Mockito.verify(objectMapper, Mockito.times(1)).readValue(anyString(), eq(ArticArtworkResponse.class));
        Mockito.verify(artworkRepository, Mockito.times(1)).save(any(Artwork.class));
    }

    @Test
    public void getArtwork_shouldThrowNotFoundWhenResponseIsNull() throws IOException, InterruptedException {
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        Mockito.when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(null);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        assertThrows(ArtworkNotFoundException.class, () -> service.getArtwork(TEST_ID));

        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
    }

    @Test
    public void getArtwork_shouldThrowNotFoundWhenResponseIsNot200() throws IOException, InterruptedException {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.NOT_FOUND.value(), "");
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        Mockito.when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        assertThrows(ArtworkNotFoundException.class, () -> service.getArtwork(TEST_ID));

        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
    }

    @Test
    public void getArtwork_shouldThrowNotFoundWhenResponseIsNot200ButRedirect() throws IOException, InterruptedException {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.PERMANENT_REDIRECT.value(), "");
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        Mockito.when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        assertThrows(ArtworkNotFoundException.class, () -> service.getArtwork(TEST_ID));

        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
    }

    @Test
    public void getArtwork_shouldThrowNotFoundWhenResponseIsNot200ButClientError() throws IOException, InterruptedException {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.NOT_FOUND.value(), "");
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        Mockito.when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        assertThrows(ArtworkNotFoundException.class, () -> service.getArtwork(TEST_ID));

        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
    }

    @Test
    public void getArtwork_shouldThrowNotFoundWhenResponseIsNot200ButServerError() throws IOException, InterruptedException {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "");
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        Mockito.when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        assertThrows(ArtworkNotFoundException.class, () -> service.getArtwork(TEST_ID));

        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
    }

    @Test
    public void getArtwork_shouldThrowNotFoundWhenIOExceptionInHttpCall() throws IOException, InterruptedException {
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        Mockito.when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenThrow(IOException.class);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        assertThrows(ArtworkNotFoundException.class, () -> service.getArtwork(TEST_ID));

        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
    }

    @Test
    public void getArtwork_shouldThrowNotFoundWhenTimeoutInHttpCall() throws IOException, InterruptedException {
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        Mockito.when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenThrow(InterruptedException.class);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        assertThrows(ArtworkNotFoundException.class, () -> service.getArtwork(TEST_ID));

        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
    }

    @Test
    public void purchaseArtwork_shouldWorkWhenArtworkIsFromDatabase() {
        User user = new User();
        user.setId(TEST_USER_ID);
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.of(getDefaultArtwork()));
        Mockito.when(userRepository.getReferenceById(TEST_USER_ID)).thenReturn(user);
        Mockito.when(userRepository.save(userArgumentCaptor.capture())).thenReturn(user);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        service.purchaseArtwork(TEST_ID, TEST_USER_ID);

        assertEquals(userArgumentCaptor.getValue().getArtworks().size(), 1);
        assertEquals(userArgumentCaptor.getValue().getArtworks().iterator().next().getId(), TEST_ID);
        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
        Mockito.verify(userRepository, Mockito.times(1)).getReferenceById(TEST_USER_ID);
        Mockito.verify(userRepository, Mockito.times(1)).save(user);
    }

    @Test
    public void purchaseArtwork_shouldWorkWhenArtworkIsFromArtic() throws IOException, InterruptedException {
        User user = new User();
        user.setId(TEST_USER_ID);
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.OK.value(), ARTIC_MOCK_JSON);
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        Mockito.when(client.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);
        Mockito.when(objectMapper.readValue(anyString(), eq(ArticArtworkResponse.class))).thenReturn(getArticArtworkResponse());
        Mockito.when(artworkRepository.save(any(Artwork.class))).thenReturn(getDefaultArtwork());
        Mockito.when(userRepository.getReferenceById(TEST_USER_ID)).thenReturn(user);
        Mockito.when(userRepository.save(userArgumentCaptor.capture())).thenReturn(user);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        service.purchaseArtwork(TEST_ID, TEST_USER_ID);

        assertEquals(userArgumentCaptor.getValue().getArtworks().size(), 1);
        assertEquals(userArgumentCaptor.getValue().getArtworks().iterator().next().getId(), TEST_ID);
        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
        HttpRequest request = httpRequestArgumentCaptor.getValue();
        assertEquals(ARTIC_API + "artworks/" + TEST_ID + "?" + FIELD_SELECTOR, request.uri().toString());
        assertTrue(request.headers().map().containsKey(USER_AGENT_HEADER));
        assertEquals(1, request.headers().map().get(USER_AGENT_HEADER).size());
        assertEquals(USER_AGENT_HEADER_VALUE, request.headers().firstValue(USER_AGENT_HEADER).get());
        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
        Mockito.verify(objectMapper, Mockito.times(1)).readValue(anyString(), eq(ArticArtworkResponse.class));
        Mockito.verify(artworkRepository, Mockito.times(1)).save(any(Artwork.class));
        Mockito.verify(userRepository, Mockito.times(1)).getReferenceById(TEST_USER_ID);
        Mockito.verify(userRepository, Mockito.times(1)).save(user);
    }

    @Test
    public void purchaseArtwork_shouldDoNothingWhenArtworkIsAlreadyPurchased() {
        User user = new User();
        user.setId(TEST_USER_ID);
        Artwork artwork = getDefaultArtwork();
        artwork.setPurchasedBy(user);
        Mockito.when(artworkRepository.findById(TEST_ID)).thenReturn(Optional.of(artwork));

        ArtworkService service = new ArtworkService(artworkRepository, userRepository);
        service.purchaseArtwork(TEST_ID, TEST_USER_ID);

        Mockito.verify(artworkRepository, Mockito.times(1)).findById(TEST_ID);
        Mockito.verify(userRepository, Mockito.never()).getReferenceById(TEST_USER_ID);
        Mockito.verify(userRepository, Mockito.never()).save(user);
    }


    @Test
    public void getArtworksPaginated_shouldWorkWithOneElement() throws IOException, InterruptedException {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.OK.value(), ARTIC_LIST_ONE_ELEMENT_MOCK_JSON);
        Mockito.when(client.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);
        Mockito.when(objectMapper.readValue(anyString(), eq(ArticArtworkListResponse.class))).thenReturn(getArticArtworkResponsesWithOneElement());

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        List<ArtworkResponse> response = service.getArtworksPaginated(PAGE_NUMBER, PAGE_SIZE);

        HttpRequest request = httpRequestArgumentCaptor.getValue();
        assertEquals(ARTIC_API + "artworks/" + "?" + FIELD_SELECTOR + "&page=" + PAGE_NUMBER + "&limit=" + PAGE_SIZE, request.uri().toString());
        assertTrue(request.headers().map().containsKey(USER_AGENT_HEADER));
        assertEquals(1, request.headers().map().get(USER_AGENT_HEADER).size());
        assertEquals(USER_AGENT_HEADER_VALUE, request.headers().firstValue(USER_AGENT_HEADER).get());

        assertEquals(1, response.size());
        assertEquals(TEST_ID, response.get(0).getId());
        assertEquals(TEST_TITLE, response.get(0).getTitle());
        assertEquals(TEST_AUTHOR, response.get(0).getAuthor());
        assertEquals(TEST_THUMBNAIL_DATA, response.get(0).getThumbnail());
        Mockito.verify(client, Mockito.times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        Mockito.verify(objectMapper, Mockito.times(1)).readValue(anyString(), eq(ArticArtworkListResponse.class));
    }

    @Test
    public void getArtworksPaginated_shouldBeEmptyWhenClientThrowsIOException() throws IOException, InterruptedException {
        Mockito.when(client.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).thenThrow(IOException.class);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        List<ArtworkResponse> response = service.getArtworksPaginated(PAGE_NUMBER, PAGE_SIZE);

        HttpRequest request = httpRequestArgumentCaptor.getValue();
        assertEquals(ARTIC_API + "artworks/" + "?" + FIELD_SELECTOR + "&page=" + PAGE_NUMBER + "&limit=" + PAGE_SIZE, request.uri().toString());
        assertTrue(request.headers().map().containsKey(USER_AGENT_HEADER));
        assertEquals(1, request.headers().map().get(USER_AGENT_HEADER).size());
        assertEquals(USER_AGENT_HEADER_VALUE, request.headers().firstValue(USER_AGENT_HEADER).get());

        assertEquals(0, response.size());
        Mockito.verify(client, Mockito.times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        Mockito.verify(objectMapper, Mockito.never()).readValue(anyString(), eq(ArticArtworkListResponse.class));
    }

    @Test
    public void getArtworksPaginated_shouldBeEmptyWhenClientTimeouts() throws IOException, InterruptedException {
        Mockito.when(client.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).thenThrow(InterruptedException.class);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        List<ArtworkResponse> response = service.getArtworksPaginated(PAGE_NUMBER, PAGE_SIZE);

        HttpRequest request = httpRequestArgumentCaptor.getValue();
        assertEquals(ARTIC_API + "artworks/" + "?" + FIELD_SELECTOR + "&page=" + PAGE_NUMBER + "&limit=" + PAGE_SIZE, request.uri().toString());
        assertTrue(request.headers().map().containsKey(USER_AGENT_HEADER));
        assertEquals(1, request.headers().map().get(USER_AGENT_HEADER).size());
        assertEquals(USER_AGENT_HEADER_VALUE, request.headers().firstValue(USER_AGENT_HEADER).get());

        assertEquals(0, response.size());
        Mockito.verify(client, Mockito.times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        Mockito.verify(objectMapper, Mockito.never()).readValue(anyString(), eq(ArticArtworkListResponse.class));
    }

    @Test
    public void getArtworksPaginated_shouldBeEmptyWhenResponseIsNull() throws IOException, InterruptedException {
        Mockito.when(client.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(null);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        List<ArtworkResponse> response = service.getArtworksPaginated(PAGE_NUMBER, PAGE_SIZE);

        HttpRequest request = httpRequestArgumentCaptor.getValue();
        assertEquals(ARTIC_API + "artworks/" + "?" + FIELD_SELECTOR + "&page=" + PAGE_NUMBER + "&limit=" + PAGE_SIZE, request.uri().toString());
        assertTrue(request.headers().map().containsKey(USER_AGENT_HEADER));
        assertEquals(1, request.headers().map().get(USER_AGENT_HEADER).size());
        assertEquals(USER_AGENT_HEADER_VALUE, request.headers().firstValue(USER_AGENT_HEADER).get());

        assertEquals(0, response.size());
        Mockito.verify(client, Mockito.times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        Mockito.verify(objectMapper, Mockito.never()).readValue(anyString(), eq(ArticArtworkListResponse.class));
    }

    @Test
    public void getArtworksPaginated_shouldBeEmptyWhenResponseIsNot200But404() throws IOException, InterruptedException {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.NOT_FOUND.value(), "");
        Mockito.when(client.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        List<ArtworkResponse> response = service.getArtworksPaginated(PAGE_NUMBER, PAGE_SIZE);

        HttpRequest request = httpRequestArgumentCaptor.getValue();
        assertEquals(ARTIC_API + "artworks/" + "?" + FIELD_SELECTOR + "&page=" + PAGE_NUMBER + "&limit=" + PAGE_SIZE, request.uri().toString());
        assertTrue(request.headers().map().containsKey(USER_AGENT_HEADER));
        assertEquals(1, request.headers().map().get(USER_AGENT_HEADER).size());
        assertEquals(USER_AGENT_HEADER_VALUE, request.headers().firstValue(USER_AGENT_HEADER).get());

        assertEquals(0, response.size());
        Mockito.verify(client, Mockito.times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        Mockito.verify(objectMapper, Mockito.never()).readValue(anyString(), eq(ArticArtworkListResponse.class));
    }

    @Test
    public void getArtworksPaginated_shouldBeEmptyWhenResponseIsNot200ButInternalServerError() throws IOException, InterruptedException {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "");
        Mockito.when(client.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        List<ArtworkResponse> response = service.getArtworksPaginated(PAGE_NUMBER, PAGE_SIZE);

        HttpRequest request = httpRequestArgumentCaptor.getValue();
        assertEquals(ARTIC_API + "artworks/" + "?" + FIELD_SELECTOR + "&page=" + PAGE_NUMBER + "&limit=" + PAGE_SIZE, request.uri().toString());
        assertTrue(request.headers().map().containsKey(USER_AGENT_HEADER));
        assertEquals(1, request.headers().map().get(USER_AGENT_HEADER).size());
        assertEquals(USER_AGENT_HEADER_VALUE, request.headers().firstValue(USER_AGENT_HEADER).get());

        assertEquals(0, response.size());
        Mockito.verify(client, Mockito.times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        Mockito.verify(objectMapper, Mockito.never()).readValue(anyString(), eq(ArticArtworkListResponse.class));
    }


    @Test
    public void getArtworksPaginated_shouldWorkWithAtLeastTwoElements() throws IOException, InterruptedException {
        MockHttpResponse mockHttpResponse = new MockHttpResponse(HttpStatus.OK.value(), ARTIC_LIST_TWO_ELEMENT_MOCK_JSON);
        Mockito.when(client.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);
        Mockito.when(objectMapper.readValue(anyString(), eq(ArticArtworkListResponse.class))).thenReturn(getArticArtworkResponsesWithTwoElement());

        ArtworkService service = new ArtworkService(artworkRepository, userRepository, client, objectMapper);
        List<ArtworkResponse> response = service.getArtworksPaginated(PAGE_NUMBER, PAGE_SIZE);

        HttpRequest request = httpRequestArgumentCaptor.getValue();
        assertEquals(ARTIC_API + "artworks/" + "?" + FIELD_SELECTOR + "&page=" + PAGE_NUMBER + "&limit=" + PAGE_SIZE, request.uri().toString());
        assertTrue(request.headers().map().containsKey(USER_AGENT_HEADER));
        assertEquals(1, request.headers().map().get(USER_AGENT_HEADER).size());
        assertEquals(USER_AGENT_HEADER_VALUE, request.headers().firstValue(USER_AGENT_HEADER).get());

        assertEquals(2, response.size());
        assertEquals(TEST_ID, response.get(0).getId());
        assertEquals(TEST_TITLE, response.get(0).getTitle());
        assertEquals(TEST_AUTHOR, response.get(0).getAuthor());
        assertEquals(TEST_THUMBNAIL_DATA, response.get(0).getThumbnail());
        assertEquals(SECOND_TEST_ID, response.get(1).getId());
        assertEquals(TEST_TITLE, response.get(1).getTitle());
        assertEquals(TEST_AUTHOR, response.get(1).getAuthor());
        assertEquals(TEST_THUMBNAIL_DATA, response.get(1).getThumbnail());
        Mockito.verify(client, Mockito.times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        Mockito.verify(objectMapper, Mockito.times(1)).readValue(anyString(), eq(ArticArtworkListResponse.class));
    }

    private Artwork getDefaultArtwork() {
        Artwork artwork = new Artwork();
        artwork.setId(TEST_ID);
        artwork.setAuthor(TEST_AUTHOR);
        artwork.setTitle(TEST_TITLE);
        artwork.setThumbnail(TEST_THUMBNAIL_DATA);
        return artwork;
    }

    private ArticArtworkResponse getArticArtworkResponse() {
        ArticThumbnailResponse thumbnailResponse = new ArticThumbnailResponse();
        thumbnailResponse.setLqip(TEST_THUMBNAIL_DATA);
        ArticArtworkResponse artworkResponse = new ArticArtworkResponse();
        artworkResponse.setId(TEST_ID);
        artworkResponse.setArtist(TEST_AUTHOR);
        artworkResponse.setTitle(TEST_TITLE);
        artworkResponse.setThumbnail(thumbnailResponse);
        return artworkResponse;
    }

    private void assertValidResponse(ArtworkResponse response) {
        Assertions.assertEquals(TEST_ID, response.getId());
        Assertions.assertEquals(TEST_AUTHOR, response.getAuthor());
        Assertions.assertEquals(TEST_TITLE, response.getTitle());
        Assertions.assertEquals(TEST_THUMBNAIL_DATA, response.getThumbnail());
    }

    private ArticArtworkListResponse getArticArtworkResponsesWithOneElement() {
        ArticArtworkListResponse response = new ArticArtworkListResponse();
        response.setData(List.of(getArticArtworkResponse()));
        return response;
    }

    private ArticArtworkListResponse getArticArtworkResponsesWithTwoElement() {
        ArticArtworkListResponse response = new ArticArtworkListResponse();
        ArticArtworkResponse second = getArticArtworkResponse();
        second.setId(SECOND_TEST_ID);
        response.setData(List.of(getArticArtworkResponse(), second));
        return response;
    }
}