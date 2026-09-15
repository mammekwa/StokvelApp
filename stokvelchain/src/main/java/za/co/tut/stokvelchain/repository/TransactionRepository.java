package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tut.stokvelchain.entity.TransactionEntity;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByMember_MemberId(UUID memberId);
    List<TransactionEntity> findBySourceTableAndSourceRecordId(String sourceTable, UUID sourceRecordId);
}
