package org.alexstpm.fms.security.repository;

import jakarta.persistence.LockModeType;
import org.alexstpm.fms.security.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from RefreshToken r
            join fetch r.user
            where r.jti = :jti
            """)
    Optional<RefreshToken> findByJtiForUpdate(@Param("jti") String jti);
}
