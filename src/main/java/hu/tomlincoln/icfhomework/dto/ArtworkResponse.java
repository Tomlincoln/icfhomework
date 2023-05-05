package hu.tomlincoln.icfhomework.dto;

public class ArtworkResponse {

    private Integer id;
    private String author;
    private String title;
    private String thumbnail;
    private Integer purchasedBy;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Integer getPurchasedBy() {
        return purchasedBy;
    }

    public void setPurchasedBy(Integer purchasedBy) {
        this.purchasedBy = purchasedBy;
    }
}
