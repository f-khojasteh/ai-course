package com.changeflow.infrastructure.persistence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
import java.util.UUID;
public interface UserAccessJpaRepository extends JpaRepository<UserAccessEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UserAccessEntity a where a.userId = :id")
    Optional<UserAccessEntity> findLocked(UUID id);
}
