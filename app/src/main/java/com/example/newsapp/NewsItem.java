package com.example.newsapp;

public class NewsItem {
    private String title, description, imageUrl, newsUrl;

    public NewsItem(String title, String description, String imageUrl, String newsUrl) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.newsUrl = newsUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getNewsUrl() {
        return newsUrl;
    }
}
