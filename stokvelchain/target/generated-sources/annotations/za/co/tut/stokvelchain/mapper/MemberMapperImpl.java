package za.co.tut.stokvelchain.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.tut.stokvelchain.dto.response.MemberResponse;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.entity.UserEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-26T21:35:32+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class MemberMapperImpl implements MemberMapper {

    @Override
    public MemberResponse toMemberResponse(MemberEntity memberEntity) {
        if ( memberEntity == null ) {
            return null;
        }

        MemberResponse.MemberResponseBuilder memberResponse = MemberResponse.builder();

        memberResponse.userId( memberEntityUserUserId( memberEntity ) );
        memberResponse.groupId( memberEntityGroupGroupId( memberEntity ) );
        memberResponse.groupName( memberEntityGroupGroupName( memberEntity ) );
        memberResponse.memberId( memberEntity.getMemberId() );
        memberResponse.fullName( memberEntity.getFullName() );
        memberResponse.nationalId( memberEntity.getNationalId() );
        memberResponse.loanRestricted( memberEntity.isLoanRestricted() );
        memberResponse.joinedAt( memberEntity.getJoinedAt() );

        return memberResponse.build();
    }

    private UUID memberEntityUserUserId(MemberEntity memberEntity) {
        UserEntity user = memberEntity.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getUserId();
    }

    private UUID memberEntityGroupGroupId(MemberEntity memberEntity) {
        StokvelGroupEntity group = memberEntity.getGroup();
        if ( group == null ) {
            return null;
        }
        return group.getGroupId();
    }

    private String memberEntityGroupGroupName(MemberEntity memberEntity) {
        StokvelGroupEntity group = memberEntity.getGroup();
        if ( group == null ) {
            return null;
        }
        return group.getGroupName();
    }
}
