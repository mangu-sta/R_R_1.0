package com.release.rr.domain.friends.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class FriendsId implements Serializable {

    private Long userId;
    private Long friendId;
}

