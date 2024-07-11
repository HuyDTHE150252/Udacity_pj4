package com.example.demo.controllers;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @GetMapping("/id/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        log.info("Get user by id: {}", id);
        Optional<User> user = userRepository.findById(id);
        return user == null ? ResponseEntity.notFound().build() : ResponseEntity.of(user);
    }

    @GetMapping("/{username}")
    public ResponseEntity<User> findByUserName(@PathVariable String username) {
        User user = null;
        try {
            log.error("Find user by username: {}", username);
            user = userRepository.findByUsername(username);
        } catch (RuntimeException e) {
            log.error("There is an error when trying to find user by name: {}", username);
            return ResponseEntity.status(500).body(null);
        }
        return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(user);
    }

    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest createUserRequest) {
        User user = new User();
        try {
            boolean isUserInfoError = Objects.isNull(createUserRequest.getPassword()) || Objects.isNull(createUserRequest.getUsername()) || createUserRequest.getPassword().isEmpty() || createUserRequest.getUsername().isEmpty();
            if (isUserInfoError) {
                log.error("Username Or Password is missing: user =  {}", createUserRequest);
                return ResponseEntity.badRequest().body(user);
            } else if (createUserRequest.getPassword().length() < 7) {
                log.error("Password must include more than 7 characters: password = {}", createUserRequest.getPassword());
                return ResponseEntity.badRequest().body(user);
            }
            user.setUsername(createUserRequest.getUsername());
            user.setPassword(bCryptPasswordEncoder.encode(createUserRequest.getPassword()));
            Cart cart = new Cart();
            cartRepository.save(cart);
            user.setCart(cart);
            userRepository.save(user);
            log.info("create user successfully: user =  {}", createUserRequest);
        } catch (RuntimeException e) {
            log.error("There is an error when trying to create user: user = {}", createUserRequest);
            return  ResponseEntity.status(500).body(user);
        }
        return ResponseEntity.ok(user);
    }

}
