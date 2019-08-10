package com.denkarz.jcat.backend.service;

import com.denkarz.jcat.backend.controller.ControllerUtils;
import com.denkarz.jcat.backend.model.user.AuthenticationUser;
import com.denkarz.jcat.backend.model.user.Role;
import com.denkarz.jcat.backend.model.user.User;
import com.denkarz.jcat.backend.repository.UserRepository;
import com.denkarz.jcat.backend.security.jwt.JwtGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.Collections;
import java.util.Optional;

@Service
@Slf4j
public class UserService implements UserDetailsService {
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private BCryptPasswordEncoder passwordEncoder;
  @Autowired
  private JwtGenerator jwtGenerator;

  @Override
  public User loadUserByUsername(String email) throws UsernameNotFoundException {
    Optional<User> userFromDb = userRepository.findByEmail(email);
    return userFromDb.orElse(null);
  }

  public ResponseEntity userRegistration(User user, BindingResult bindingResult) {
    Optional<User> userFromDb = userRepository.findByEmail(user.getEmail());
    if (userFromDb.isPresent()) {
      // ToDo: add logger for error
      return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"emailError\": \"email_in_use\"}");
    }
    user.setActive(true);
    // ToDo: replace for multiple roles
    user.setRoles(Collections.singleton(Role.USER));
    if (bindingResult.hasErrors()) {
      log.warn(ControllerUtils.getErrorsLog(bindingResult));
      return ResponseEntity.status(HttpStatus.CONFLICT).body(ControllerUtils.getErrorsResponse(bindingResult));
    }
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    User savedUser = userRepository.save(user);
    // todo: refactor of duplicating id into nickname
    savedUser.setNickname(savedUser.getId());
    savedUser = userRepository.save(savedUser);
    String jwt = jwtGenerator.generate(savedUser);
    log.info("Add new user to database: {}", savedUser);
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add("Authorisation", "Token " + jwt);
    return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(null);
  }

  public boolean hasMail(String email) {
    email = email.trim().toLowerCase();
    if (!email.isBlank()) {
      Optional<User> userFromDb = userRepository.findByEmail(email);
      return userFromDb.isEmpty();
    }
    return false;
  }

  public ResponseEntity userLogin(AuthenticationUser authUser, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      log.warn(ControllerUtils.getErrorsLog(bindingResult));
      return ResponseEntity.status(HttpStatus.CONFLICT).body(ControllerUtils.getErrorsResponse(bindingResult));
    }
    authUser.setEmail(authUser.getEmail().trim().toLowerCase());
    Optional<User> userFromDb = userRepository.findByEmail(authUser.getEmail());
    if (userFromDb.isPresent() && passwordEncoder.matches(authUser.getPassword(), userFromDb.get().getPassword())) {
      // ToDo: add logger for error

      userFromDb.get().setActive(true);
      String jwt = jwtGenerator.generate(userFromDb.get());
      log.info("Login as {}", userFromDb.get());
      HttpHeaders responseHeaders = new HttpHeaders();
//      responseHeaders.add("Access-Control-Expose-Headers", "Accept, Origin, Content-Type, Authorisation");
      responseHeaders.add("Authorisation", "Token " + jwt);
      return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(null);
    }
    return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"emailError\": \"user_not_found\"}");
  }

  public Iterable<User> testRequest() {
    return userRepository.findAll();
  }

  public ResponseEntity registrationSetup(AuthenticationUser authUser, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      log.warn(ControllerUtils.getErrorsLog(bindingResult));
      return ResponseEntity.status(HttpStatus.CONFLICT).body(ControllerUtils.getErrorsResponse(bindingResult));
    }
    authUser.setEmail(authUser.getEmail().trim().toLowerCase());
    if (this.hasMail(authUser.getEmail())) {
      return ResponseEntity.status(HttpStatus.OK).body("ok");
    }
    return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"emailError\": \"email_in_use\"}");
  }

  public ResponseEntity getUser(String id) {
    Optional<User> userFromDb = userRepository.findById(id);
    if (userFromDb.isPresent()) {
      return ResponseEntity.status(HttpStatus.OK).body(userFromDb);
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("user_not_found");
  }

  public ResponseEntity logout(String id) {
    Optional<User> userFromDb = userRepository.findById(id);
    if (userFromDb.isPresent()) {
      User user = userFromDb.get();
      user.setActive(false);
      userRepository.save(user);
      return ResponseEntity.status(HttpStatus.OK).body("ok");
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("user_not_found");
  }
}
