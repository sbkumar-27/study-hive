package com.studyhive.backend.dto.community;

/** Password is only needed when the community has one. */
public record ShareLinkAccessRequest(String password) {
}
