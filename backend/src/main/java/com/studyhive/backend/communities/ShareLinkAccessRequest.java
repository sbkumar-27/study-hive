package com.studyhive.backend.communities;

/** Password is only needed when the community has one. */
public record ShareLinkAccessRequest(String password) {
}
