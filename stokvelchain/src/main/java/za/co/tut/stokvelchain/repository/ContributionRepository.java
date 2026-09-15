package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tut.stokvelchain.entity.ContributionEntity;

import java.util.List;
import java.util.UUID;

public interface ContributionRepository extends JpaRepository<ContributionEntity, UUID> {
    List<ContributionEntity> findByMember_MemberId(UUID memberId);
}
