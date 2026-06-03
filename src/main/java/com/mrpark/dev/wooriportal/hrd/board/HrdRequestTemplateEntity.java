package com.mrpark.dev.wooriportal.hrd.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 하베스팅된 HRD 요청 본문 템플릿(인증서·gds_userInfo 포함)을 영속 저장한다.
 *
 * <p>엔드포인트별 1행(detail/list). HRD 를 실제로 쓰는 사람의 요청이 프록시를 지나면
 * 그 요청 본문(=그 사람의 인증서)이 여기에 갱신된다 → 특정 개인 인증서 만료에
 * 묶이지 않고 "최근 사용자"의 자격으로 자동 동작.</p>
 */
@Entity
@Table(name = "hrd_request_template")
@Getter @Setter
public class HrdRequestTemplateEntity {

    /** 엔드포인트 키: "detail"(selectDailAtndceDetail) | "list"(selectAtendList) */
    @Id
    @Column(name = "endpoint", length = 20)
    private String endpoint;

    /** 원본 요청 본문(FF AD + zlib). */
    @Lob
    @Column(name = "body", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] body;

    /** 템플릿 제공자(인증서 소유자) 이름 — gds_userInfo.usrNm 에서 추출. */
    @Column(name = "owner_name", length = 100)
    private String ownerName;

    /** 하베스터 source(어느 PC/계정에서 수확). */
    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
