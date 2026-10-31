package com.epfjava.horsecopilote.common.service;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " with id " + id + " not found");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
