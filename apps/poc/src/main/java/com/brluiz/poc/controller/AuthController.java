package com.brluiz.poc.controller;

import com.brluiz.poc.dto.LoginDTO;
import com.brluiz.poc.dto.RegisterDTO;
import com.brluiz.poc.dto.TokenDTO;
import com.brluiz.poc.entity.User;
import com.brluiz.poc.repository.UserRepository;
import com.brluiz.poc.security.JwtUtil;
import com.brluiz.poc.service.NotificationServiceClient;
import com.brluiz.poc.service.TicketServiceClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TicketServiceClient ticketServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                        UserRepository userRepository, PasswordEncoder passwordEncoder,
                        TicketServiceClient ticketServiceClient,
                        NotificationServiceClient notificationServiceClient) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.ticketServiceClient = ticketServiceClient;
        this.notificationServiceClient = notificationServiceClient;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUsername(),
                            loginDTO.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            ticketServiceClient.incrementTicket("AUDITORIA");

            return ResponseEntity.ok(new TokenDTO(token, "Bearer"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciais inválidas");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO registerDTO) {
        try {
            // Validar se o usuário já existe
            if (userRepository.findByUsername(registerDTO.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Usuário já existe");
            }

            if (userRepository.findByEmail(registerDTO.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Email já cadastrado");
            }

            // Criar novo usuário
            User newUser = new User(
                    registerDTO.getUsername(),
                    passwordEncoder.encode(registerDTO.getPassword()),
                    registerDTO.getEmail(),
                    User.Role.USER
            );

            userRepository.save(newUser);

            ticketServiceClient.incrementTicket("NOTIFICACAO");
            notificationServiceClient.sendWelcomeEmail(registerDTO.getEmail(), registerDTO.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Usuário cadastrado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao registrar usuário");
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                boolean isValid = jwtUtil.validateToken(token);

                return ResponseEntity.ok(new TokenDTO(token, isValid ? "Valid" : "Invalid"));
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Authorization header ausente ou inválido");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token inválido");
        }
    }
}
