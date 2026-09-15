package za.co.tut.stokvelchain.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.tut.stokvelchain.dto.response.ContributionResponse;
import za.co.tut.stokvelchain.entity.ContributionEntity;
import za.co.tut.stokvelchain.entity.MemberEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-14T20:50:20+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ContributionMapperImpl implements ContributionMapper {

    @Override
    public ContributionResponse toResponse(ContributionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ContributionResponse.ContributionResponseBuilder contributionResponse = ContributionResponse.builder();

        contributionResponse.memberId( entityMemberMemberId( entity ) );
        contributionResponse.contributionId( entity.getContributionId() );
        contributionResponse.amount( entity.getAmount() );
        contributionResponse.contributionDate( entity.getContributionDate() );
        contributionResponse.txHash( entity.getTxHash() );
        contributionResponse.blockchainConfirmed( entity.isBlockchainConfirmed() );
        contributionResponse.createdAt( entity.getCreatedAt() );

        return contributionResponse.build();
    }

    private UUID entityMemberMemberId(ContributionEntity contributionEntity) {
        MemberEntity member = contributionEntity.getMember();
        if ( member == null ) {
            return null;
        }
        return member.getMemberId();
    }
}
