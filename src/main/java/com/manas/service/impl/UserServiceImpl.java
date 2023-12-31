package com.manas.service.impl;

import com.manas.config.jwt.JwtService;
import com.manas.dto.request.AuthenticationRequest;
import com.manas.dto.request.RegisterRequest;
import com.manas.dto.response.AuthenticationResponse;
import com.manas.entity.User;
import com.manas.enums.Role;
import com.manas.exceptions.AlreadyExistException;
import com.manas.exceptions.NotFoundException;
import com.manas.repository.UserRepository;
import com.manas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())){
            throw new AlreadyExistException(String.format("User with email %s already exists!", request.email()));
        }
        User user = User.builder()
                .fullName(request.fullName())
                .image(request.image())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        String jwt = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .email(user.getEmail())
                .token(jwt)
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        if (!userRepository.existsByEmail(request.email())){
            throw new NotFoundException(String.format("User with email %s doesn't exist!", request.email()));
        }
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(()->new UsernameNotFoundException(
                String.format("User with email %s doesn't exist!", request.email()))
        );
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getImage(), user.getPassword()));
        String token = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .email(user.getEmail())
                .token(token)
                .role(user.getRole().name())
                .build();
    }
}
