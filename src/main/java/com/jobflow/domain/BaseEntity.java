package com.jobflow.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {
    private Long id;
    private Long tenantId;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;
    private Boolean deleted;
    private Integer version;

    public void prePersist() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        this.deleted = false;
        this.version = 1;
        if (this.tenantId == null) {
            this.tenantId = 0L; // Default tenant
        }
    }

    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
        this.version = this.version + 1;
    }
}
