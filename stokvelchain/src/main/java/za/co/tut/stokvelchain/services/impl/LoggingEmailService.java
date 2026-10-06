package za.co.tut.stokvelchain.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.tut.stokvelchain.services.EmailService;

@Slf4j
@Service
public class LoggingEmailService implements EmailService {

    public void sendGroupInvitation(String toEmail, String inviterName, String groupName, String inviteLink) {
        log.info("[DEV EMAIL] To: {} | {} invited you to join '{}' on StokvelChain: {}",
                toEmail, inviterName, groupName, inviteLink);
    }
}
