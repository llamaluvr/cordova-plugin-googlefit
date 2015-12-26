package de.ilpt.plugins.googlefit;

interface Loggable {

    void log(String message);

    void log(String message, Exception e);
}