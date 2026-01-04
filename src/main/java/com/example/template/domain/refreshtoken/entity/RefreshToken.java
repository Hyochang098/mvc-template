package com.example.template.domain.refreshtoken.entity;

import com.example.template.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", uniqueConstraints = {
    @UniqueConstraint(name = "uk_refresh_tokens_user_id", columnNames = "userId")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long refreshTokenId;

  @Column(nullable = false, unique = true)
  private Long userId;

  @Column(nullable = false)
  private String tokenHash;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @PrePersist
  protected void onCreate() {
    if (expiresAt == null) {
      expiresAt = LocalDateTime.now().plusDays(7);
    }
  }

  public void updateToken(String newTokenHash, LocalDateTime newExpiresAt) {
    this.tokenHash = newTokenHash;
    this.expiresAt = newExpiresAt;
  }

  public boolean isExpired(LocalDateTime now) {
    return expiresAt != null && expiresAt.isBefore(now);
  }
}
