package za.co.tut.stokvelchain.services;

public interface EmailService {
    void sendGroupInvitation(String toEmail, String inviterName, String groupName, String inviteLink);
}
