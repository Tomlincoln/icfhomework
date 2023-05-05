package hu.tomlincoln.icfhomework.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArticThumbnailResponse {

    private String lqip;
    private Integer width;
    private Integer height;
    @JsonProperty("alt_text")
    private String altText;

    public String getLqip() {
        return lqip;
    }

    public void setLqip(String lqip) {
        this.lqip = lqip;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }
}
