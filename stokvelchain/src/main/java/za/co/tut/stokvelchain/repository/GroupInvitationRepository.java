package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tut.stokvelchain.entity.GroupInvitationEntity;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.enums.InvitationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitationEntity, UUID> {

    Optional<GroupInvitationEntity> findByToken(String token);

    List<GroupInvitationEntity> findByGroupAndEmailIgnoreCaseAndStatus(
            StokvelGroupEntity group, String email, InvitationStatus status);
}
