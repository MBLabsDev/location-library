package br.com.mblabs.service;

import android.app.NotificationChannel;
import android.app.PendingIntent;

public class ForegroundNotification {

    private String title;
    private String text;
    private PendingIntent intent;
    private int resourceId;
    private int id;
    private boolean ongoing;
    private int color;
    private NotificationChannel channel;
    private String channelName;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public PendingIntent getIntent() {
        return intent;
    }

    public void setIntent(PendingIntent intent) {
        this.intent = intent;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isOngoing() {
        return ongoing;
    }

    public void setOngoing(boolean ongoing) {
        this.ongoing = ongoing;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
}