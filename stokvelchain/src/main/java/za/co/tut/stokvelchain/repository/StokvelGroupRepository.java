package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;

import java.util.UUID;

@Repository
public interface StokvelGroupRepository extends JpaRepository<StokvelGroupEntity, UUID> {

}
