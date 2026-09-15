package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tut.stokvelchain.entity.MemberEntity;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, UUID> {
    Optional<MemberEntity> findByUser_UserId(UUID userId);

    Optional <MemberEntity> findByUser_Email(String email);

    Optional<MemberEntity> findByNationalId(String nationalId);

    boolean existsByNationalId(String nationalId);
}
