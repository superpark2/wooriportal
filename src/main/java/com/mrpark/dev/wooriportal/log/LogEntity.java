package com.mrpark.dev.wooriportal.log;

import com.mrpark.dev.wooriportal.global.base.BaseEntity;
import com.mrpark.dev.wooriportal.pcinfo.PcInfoEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "log")
@Getter
@Setter
public class LogEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    // 버그수정: createdAt 수동 관리(@PrePersist) 제거 → BaseEntity.createdAt 재사용
    // (BaseEntity를 상속하면 @CreatedDate로 자동 관리됨)

    @ManyToOne
    @JoinColumn(name = "pcinfo_num", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private PcInfoEntity pcInfo;
}
