package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.request.LoginRequest;
import za.co.tut.stokvelchain.dto.request.RegisterRequest;
import za.co.tut.stokvelchain.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
