package za.co.tut.stokvelchain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import za.co.tut.stokvelchain.dto.response.ContributionResponse;
import za.co.tut.stokvelchain.entity.ContributionEntity;

@Mapper(componentModel = "spring")
public interface ContributionMapper {
    @Mapping(source = "member.memberId", target = "memberId")
    ContributionResponse toResponse(ContributionEntity entity);
}
