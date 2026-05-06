package com.tex.cloud_task_manager.RefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {


    @Modifying
    @Query("""
    update RefreshTokenEntity rt
    set rt.revoked = true,
        rt.revokedAt = :revokedAt
    where rt.tokenHash = :tokenHash
      and rt.revoked = false
    """)
    int revokeActiveTokensByTokenHash(@Param("tokenHash") String tokenHash, @Param("revokedAt") LocalDateTime revokedAt);

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    RefreshTokenEntity findByTokenHashAndRevoked(String tokenHash, Boolean revoked);

    // @Modifying
    // @Query("""
    // update RefreshTokenEntity rt
    // set rt.revoked = true
    // where rt.tokenHash = :tokenHash
    // """)
    // int revokeByTokenHash(@Param("tokenHash") String tokenHash);



    // @Modifying
    // @Query("""
    // update RefreshTokenEntity rt
    // set rt.revoked = true
    // where rt.userId = :userId
    // and rt.revoked = false
    // """)
    // int revokeByUserId(@Param("userId") Long userId);


    // Optional<RefreshTokenEntity> findByUserIdAndRevoked(Long userId, Boolean revoked);

}
