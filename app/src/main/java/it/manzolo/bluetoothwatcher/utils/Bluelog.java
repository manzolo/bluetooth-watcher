package it.manzolo.bluetoothwatcher.utils;

public class Bluelog {
    private String data;
    private String message;
    private String type;

    public Bluelog(String data, String message, String type) {
        this.data = data;
        this.message = message;
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public interface logEvents {
        String ERROR = "E";
        String WARNING = "W";
        String INFO = "I";
        String BROADCAST = "BROADCASTMESSAGE";
    }

}