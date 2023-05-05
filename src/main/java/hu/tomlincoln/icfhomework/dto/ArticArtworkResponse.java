package hu.tomlincoln.icfhomework.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArticArtworkResponse {

    private Integer id;
    private String title;
    @JsonProperty("artist_title")
    private String artist;
    private ArticThumbnailResponse thumbnail;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public ArticThumbnailResponse getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(ArticThumbnailResponse thumbnail) {
        this.thumbnail = thumbnail;
    }
}
