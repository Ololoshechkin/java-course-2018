package ru.ifmo.rain.brilyantov.walk;

public class BadPathException extends RuntimeException {
    private String message;
    private String path;
    private Integer lineNumber;
    BadPathException(String path, Integer lineNumber, String message) {
        this.message = message;
    }
    public String getMessage() {
        return "In file \"" + path + "\" on line " + lineNumber + " error \"" + message + "\"";
    }
}
