package com.release.rr.domain.group_members.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class GroupMemberId implements Serializable {

    private Long mapId;
    private Long userId;
}
