package com.studyhive.backend.communities;

import jakarta.validation.constraints.Size;

/** Null or blank clears the pinned note. */
public record UpdateCommunityPinnedNoteRequest(@Size(max = 2000) String pinnedNote) {
}
