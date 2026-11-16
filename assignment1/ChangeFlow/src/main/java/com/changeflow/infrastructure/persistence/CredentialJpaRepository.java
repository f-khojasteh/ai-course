package com.changeflow.infrastructure.persistence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
import java.util.UUID;
public interface CredentialJpaRepository extends JpaRepository<CredentialEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CredentialEntity c where c.credentialId = :id")
    Optional<CredentialEntity> findLocked(UUID id);
}
