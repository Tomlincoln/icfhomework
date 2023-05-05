package hu.tomlincoln.icfhomework.dto;

import java.util.List;

public class ArticArtworkListResponse {

    private Object pagination;
    private List<ArticArtworkResponse> data;
    private Object info;
    private Object config;

    public Object getPagination() {
        return pagination;
    }

    public void setPagination(Object pagination) {
        this.pagination = pagination;
    }

    public List<ArticArtworkResponse> getData() {
        return data;
    }

    public void setData(List<ArticArtworkResponse> data) {
        this.data = data;
    }

    public Object getInfo() {
        return info;
    }

    public void setInfo(Object info) {
        this.info = info;
    }

    public Object getConfig() {
        return config;
    }

    public void setConfig(Object config) {
        this.config = config;
    }
}
