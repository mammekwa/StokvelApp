package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.entity.UserEntity;
import za.co.tut.stokvelchain.enums.MemberStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, UUID> {
    Optional<MemberEntity> findByUserAndGroup(UserEntity user, StokvelGroupEntity group);
    Optional <MemberEntity> findByUser_Email(String username);
    List<MemberEntity> findByUserAndStatus(UserEntity user, MemberStatus status);

    List<MemberEntity> findByGroupAndStatusOrderByPayoutPositionAsc(StokvelGroupEntity group, MemberStatus status);

    List<MemberEntity> findByGroupAndStatusOrderByJoinedAtAsc(StokvelGroupEntity group, MemberStatus status);

    long countByGroupAndStatus(StokvelGroupEntity group, MemberStatus status);

    @Query("select coalesce(max(m.payoutPosition), 0) from MemberEntity m where m.group = :group")
    int findMaxPayoutPosition(@Param("group") StokvelGroupEntity group);
}
