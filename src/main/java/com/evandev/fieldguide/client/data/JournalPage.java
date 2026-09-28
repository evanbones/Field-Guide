package com.evandev.fieldguide.client.data;

public class JournalPage {
    public String title;
    public String content;
    public long timestamp;

    public JournalPage(String title, String content, long timestamp) {
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }
}
