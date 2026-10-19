package za.co.tut.stokvelchain.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tut.stokvelchain.dto.request.LoanApplicationRequest;
import za.co.tut.stokvelchain.entity.BehavioralScoreEntity;
import za.co.tut.stokvelchain.entity.LoanEntity;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.entity.UserEntity;
import za.co.tut.stokvelchain.enums.MemberStatus;
import za.co.tut.stokvelchain.exception.LoanEligibilityException;
import za.co.tut.stokvelchain.repository.*;
import za.co.tut.stokvelchain.scoring.BehavioralFeatureCalculator;
import za.co.tut.stokvelchain.scoring.CreditScoringClient;
import za.co.tut.stokvelchain.services.BlockchainService;
import za.co.tut.stokvelchain.services.CurrentUserService;
import za.co.tut.stokvelchain.services.NotificationService;
import za.co.tut.stokvelchain.services.impl.LoanServiceImpl;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private BehavioralScoreRepository behavioralScoreRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ContributionRepository contributionRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private StokvelGroupRepository groupRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private BehavioralFeatureCalculator featureCalculator;
    @Mock private CreditScoringClient creditScoringClient;
    @Mock private BlockchainService blockchainService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private LoanServiceImpl loanService;

    private UUID groupId;
    private UserEntity user;
    private MemberEntity member;
    private LoanApplicationRequest request;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();

        user = UserEntity.builder()
                .userId(UUID.randomUUID())
                .email("thabo@example.com")
                .build();

        member = MemberEntity.builder()
                .memberId(UUID.randomUUID())
                .fullName("Thabo Mokoena")
                .status(MemberStatus.ACTIVE)
                .loanRestricted(false)
                .walletAddress(
                        "0x0000000000000000000000000000000000000001"
                )
                .build();

        request = LoanApplicationRequest.builder()
                .amount(new java.math.BigDecimal("1000.00"))
                .repaymentMonths(6)
                .build();
    }

    @Test
    void applyForLoan_shouldRejectMemberWithFewerThanThreeConfirmedContributions() {

        // Arrange: identify the authenticated member.
        when(currentUserService.get()).thenReturn(user);

        when(memberRepository.findByUser_EmailAndGroup_GroupId(
                user.getEmail(), groupId))
                .thenReturn(Optional.of(member));

        // The member has only two confirmed contributions.
        when(contributionRepository
                .countByMember_MemberIdAndBlockchainConfirmedTrue(
                        member.getMemberId()))
                .thenReturn(2L);

        // Act and Assert: the loan application must be rejected.
        assertThatThrownBy(
                () -> loanService.applyForLoan(groupId, request))
                .isInstanceOf(LoanEligibilityException.class)
                .hasMessageContaining(
                        "At least 3 confirmed contributions are required");

        // Verify that processing stopped before scoring or saving.
        verify(featureCalculator, never())
                .calculate(any(MemberEntity.class), any());

        verifyNoInteractions(creditScoringClient);

        verify(loanRepository, never())
                .saveAndFlush(any(LoanEntity.class));

        verify(behavioralScoreRepository, never())
                .save(any(BehavioralScoreEntity.class));
    }
}

