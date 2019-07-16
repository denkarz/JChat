package com.denkarz.jcat.backend.service;

import com.denkarz.jcat.backend.dto.AuthenticationRequestDto;
import com.denkarz.jcat.backend.model.Role;
import com.denkarz.jcat.backend.model.User;
import com.denkarz.jcat.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Optional<User> userFromDb = userRepository.findByEmail(email);
    return userFromDb.orElse(null);
  }

  public ResponseEntity userRegistration(User user) {
    Optional<User> userFromDb = userRepository.findByEmail(user.getEmail());
    if (userFromDb.isPresent()) {
      // ToDo: add logger for error
      return ResponseEntity.status(HttpStatus.CONFLICT).body("user_exists");
    }
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setActive(true);
    user.setNickname(user.getId());
    // ToDo: replace for multiple roles
    user.setRoles(Collections.singleton(Role.USER));
    // ToDo: add logger for success
    try {
      User savedUser = userRepository.save(user);
      return ResponseEntity.status(HttpStatus.OK).body(savedUser.getId());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("validation_error");
    }
  }

  public boolean hasMail(String email) {
    email = email.toLowerCase();
    Optional<User> userFromDb = userRepository.findByEmail(email);
    // ToDo: add logger for error
    return userFromDb.isEmpty();
  }

  public ResponseEntity userLogin(AuthenticationRequestDto authUser) {
    Optional<User> userFromDb = userRepository.findByEmail(authUser.getEmail());
    if (userFromDb.isPresent() && passwordEncoder.matches(authUser.getPassword(), userFromDb.get().getPassword())) {
      // ToDo: add logger for error
      userFromDb.get().setActive(true);
      return ResponseEntity.status(HttpStatus.OK).body(userFromDb);
    }
    return ResponseEntity.status(HttpStatus.CONFLICT).body("user_not_exists");
  }

  public Iterable<User> testRequest() {
    return userRepository.findAll();
  }
}