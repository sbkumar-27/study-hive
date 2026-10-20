package com.studyhive.backend.entity;

import java.io.Serializable;
import java.util.Objects;

public class MaterialFavoriteId implements Serializable {

    private Long user;
    private Long material;

    public MaterialFavoriteId() {
    }

    public MaterialFavoriteId(Long user, Long material) {
        this.user = user;
        this.material = material;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialFavoriteId that)) return false;
        return Objects.equals(user, that.user) && Objects.equals(material, that.material);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, material);
    }
}
