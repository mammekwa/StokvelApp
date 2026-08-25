package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse getUserById(UUID userId);
}
