package com.function;

public class QueueMappingRequest {
    private String event;

    private Object message;

    // Constructors, getters, and setters
    public QueueMappingRequest() {
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }
}
