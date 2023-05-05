package hu.tomlincoln.icfhomework.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import hu.tomlincoln.icfhomework.dto.ArtworkResponse;
import hu.tomlincoln.icfhomework.entity.User;
import hu.tomlincoln.icfhomework.exception.ArtworkNotFoundException;
import hu.tomlincoln.icfhomework.service.ArtworkService;

@RestController
public class ArtworkController {

    private final static Logger LOGGER = LoggerFactory.getLogger(ArtworkController.class);
    private final ArtworkService artworkService;

    @Autowired
    public ArtworkController(ArtworkService artworkService) {
        this.artworkService = artworkService;
    }

    @GetMapping("/artwork/{id}")
    public ResponseEntity<ArtworkResponse> getArtwork(@PathVariable("id") Integer id) {
        try {
            return ResponseEntity.ok(artworkService.getArtwork(id));
        } catch (ArtworkNotFoundException e) {
            LOGGER.warn("Artwork not found with ID {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/artwork/{id}/purchase")
    public ResponseEntity<Void> purchaseArtwork(@PathVariable("id") Integer id, Authentication authentication) {
        try {
            Integer userId = ((User) authentication.getPrincipal()).getId();
            artworkService.purchaseArtwork(id, userId);
            return ResponseEntity.ok().build();
        } catch (ArtworkNotFoundException e) {
            LOGGER.warn("Artwork not found with ID {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/artwork/page/{pageNum}/limit/{pageSize}")
    public ResponseEntity<List<ArtworkResponse>> getArtworksPaginated(@PathVariable("pageNum") Integer pageNum, @PathVariable("pageSize") Integer pageSize) {
        try {
            return ResponseEntity.ok(artworkService.getArtworksPaginated(pageNum, pageSize));
        } catch (ArtworkNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


}
