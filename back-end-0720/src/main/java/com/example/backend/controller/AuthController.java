package com.example.backend.controller;

import com.example.backend.model.*;
import com.example.backend.payload.request.LoginRequest;
import com.example.backend.payload.request.SignupRequest;
import com.example.backend.payload.response.JwtResponse;
import com.example.backend.payload.response.MessageResponse;
import com.example.backend.security.jwt.JwtUtils;
import com.example.backend.security.services.UserDetailsImpl;
import com.example.backend.service.RoleServiceImpl;
import com.example.backend.service.SendEmailServiceImp;
import com.example.backend.service.UserServiceImpl;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserServiceImpl userService;

    @Autowired
    RoleServiceImpl roleService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    SendEmailServiceImp sendEmailService;

    // ????????? ?????????
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid
                                              @RequestBody
                                              LoginRequest loginRequest) {

        //  ????????? ??????, ???????????? ??????
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        //  SecurityContext ??? ???????????? ???????????? Authentication
        SecurityContextHolder.getContext().setAuthentication(authentication);
        //  JWT ?????? ??????
        String jwt = jwtUtils.generateJwtToken(authentication);

        //  ?????? ?????? ??????( UserDetails ) ????????????
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        //  ?????? ?????? JWT ??? UserDetails ?????????
        List<String> role = userDetails.getAuthorities()
                                       .stream()
                                       .map(item -> item.getAuthority())
                                       .collect(Collectors.toList());

        //    ?????????
        /*roles.add("ROLE_USER");*/


        logger.info("loginRequest authenticateUser {} ", role);

        return ResponseEntity.ok(
                new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getName(),
                                userDetails.getAddress(), userDetails.getEmail(), role, userDetails.getPhone(),
                                userDetails.getDeleteYn(), userDetails.getInsertTime(), userDetails.getUpdateTime(),
                                userDetails.getDeleteTime()));
    }

    // ????????????
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid
                                          @RequestBody
                                          SignupRequest signUpRequest) {

        logger.info("signUpRequest {} ", signUpRequest);

        if (userService.existsByUserName(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userService.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(),
                             encoder.encode(signUpRequest.getPassword()), signUpRequest.getName(),
                             signUpRequest.getAddress(), signUpRequest.getPhone(), signUpRequest.getDeleteYn(),
                             signUpRequest.getInsertTime(), signUpRequest.getUpdateTime(),
                             signUpRequest.getDeleteTime());

//    Set<String> strRoles = signUpRequest.getRoles();
        String strRoles = signUpRequest.getRole();
//    Set<Role> roles = new HashSet<>();
        Set<String> roles = new HashSet<>();

        // * ????????????????????? strRoles ???, ????????? ????????? ?????? ???
        if (strRoles == null) {
            Role userRole = roleService.findByName(ERole.ROLE_USER)
                                       .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole.getName().name());
//      user ?????? ??????
            user.setRole(userRole.getName().name());
        }
        // * ????????????????????? strRoles ???, ????????? ????????? ?????? ???
        else {
//      strRoles.forEach(role -> {
            switch (strRoles) {
                case "ROLE_ADMIN":
                    Role adminRole = roleService.findByName(ERole.ROLE_ADMIN)
                                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(adminRole.getName().name());
//      user ?????? ??????
                    user.setRole(adminRole.getName().name());
                    break;
                /*case "ROLE_MODERATOR":
                    Role modRole = roleService.findByName(ERole.ROLE_MODERATOR)
                                              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(modRole.getName().name());
//      user ?????? ??????
                    user.setRole(modRole.getName().name());
                    break;*/
                default:
                    Role userRole = roleService.findByName(ERole.ROLE_USER)
                                               .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(userRole.getName().name());
                    //      user ?????? ??????
                    user.setRole(userRole.getName().name());
            }
//      });
        }

        user.setRoles(roles);
        userService.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    // ?????? ??????(update)
    @PutMapping("/user-deletion/{id}")
    public ResponseEntity<HttpStatus> deleteUser(
            @PathVariable("id")
            Long id) {

        boolean bSuccess = userService.deleteUser(id);

        logger.info("bSuccess {}", bSuccess);

        try {
//			boolean bSuccess = tutorialService.deleteById(id);

            if (bSuccess) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

/*    // ???????????? ??????1
    @PutMapping("/user-update")
    public ResponseEntity<?> updateUser(@Valid
                                        @RequestBody
                                        SignupRequest signUpRequest) {

        logger.info("signUpRequest {} ", signUpRequest);
        Optional<User> user = userService.findByUserName(signUpRequest.getUsername());

        logger.info("USER {} ", user);
        // user??? ???????????? ?????? ???
        // *!user.isPresent() => user == null
        if (!user.isPresent()) {
            return ResponseEntity.badRequest()
                                 .body(new MessageResponse("Error : User Is Not Exist"));
        }

        // ?????? ??????????????? ????????? ??? ???
        if (!encoder.matches(signUpRequest.getPassword(), user.get().getPassword())) {
            return ResponseEntity.badRequest()
                                 .body(new MessageResponse("Error : Current Password Different!"));
        }

        // ?????? ??????????????? ????????? ???
        try {
            // ???????????? ?????? ?????? ??????
            User updateUser = new User(signUpRequest.getUsername(), signUpRequest.getEmail(),
                                       encoder.encode(signUpRequest.getPassword()), signUpRequest.getName(),
                                       signUpRequest.getAddress(), signUpRequest.getPhone(),
                                       signUpRequest.getDeleteYn(), signUpRequest.getInsertTime(),
                                       signUpRequest.getUpdateTime(),
                                       signUpRequest.getDeleteTime());

            // ??? ??????????????? ????????? ??? ??????????????? ??????
            if (signUpRequest.getNewPassword() != null) {
                updateUser.setPassword(encoder.encode(signUpRequest.getNewPassword()));
            }

            logger.info("updateUser {} ", updateUser);

            // ???????????? ??????
            userService.updateUser(updateUser);
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest()
                                 .body(new MessageResponse("Error : User registered Failure!"));
        }
    }
}*/

    // ???????????? ??????2
    @PutMapping("/user-update")
    public ResponseEntity<?> updateUser(@Valid
                                        @RequestBody
                                        SignupRequest signUpRequest) {
        logger.info("signUpRequest {} ", signUpRequest);
        Optional<User> user = userService.findByUserName(signUpRequest.getUsername());

        logger.info("USER {} ", user);
        // user??? ???????????? ?????? ???
        // *!user.isPresent() => user == null
        if (!user.isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error : User Is Not Exist"));
        }

        // ?????? ??????????????? ????????? ??? ???
        if (!encoder.matches(signUpRequest.getPassword(), user.get().getPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error : Current Password Different!"));
        }

        // ?????? ??????????????? ????????? ???
        try {
            // ??? ??????????????? ????????? ??? ??????????????? ??????
            // *??? ???????????? ?????? ???????????? ???????????? .isEmpty()??? ??????
            if (!signUpRequest.getNewPassword().isEmpty()) {
                logger.info("test {} ", signUpRequest.getNewPassword());
                user.get().setPassword(encoder.encode(signUpRequest.getNewPassword()));
            }
            // ???????????? ?????? ?????????
            user.get().setEmail(signUpRequest.getEmail());
            user.get().setAddress(signUpRequest.getAddress());
            user.get().setPhone(signUpRequest.getPhone());

            logger.info("updateUser {} ", user);

            // ???????????? ??????
            userService.updateUser(user.get());
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error : User registered Failure!"));
        }
    }

    // ????????? ??????
    /*@PostMapping("/mail")
    public ResponseEntity<?> sendEmail(
            @RequestBody
            SendEmailDto sendEmailDto, HttpServletResponse response) {
        logger.info("mail??? ?????????????????????.");
        logger.info("sendEmailDto:{}", sendEmailDto);

        try {
            User userID = sendEmailService.findID(sendEmailDto.getName(), sendEmailDto.getEmail());
            logger.info("userID : {}", userID);

            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out;
            // SimpleEmail ???????????? ??????
            SimpleEmail email = new SimpleEmail();
            email.setHostName("smtp.naver.com");
            email.setSmtpPort(587);
            email.setAuthentication("littlegbear", "K1GLJB4ENX3M");

            // ???????????? SSL, TLS ?????? ??????
            email.setSSLOnConnect(true);
            email.setStartTLSEnabled(true);

            logger.info("?????? ?????????");
            // ????????? ?????? ?????? (SMTP ????????? ????????? ?????? ???????????? ???????????? ???????????? ??????!)
            email.setFrom("littlegbear@naver.com", "littlegbear", "utf-8");
            // ?????? ?????? ??????
            *//*email.addTo(userID.get().getEmail(), userID.get().getName(), "utf-8");*//*
            email.addTo("littlegbear@naver.com", userID.getName(), "utf-8");
            // ?????? ??????
            email.setSubject("??????????????????.");

            String ePw = sendEmailService.createKey();
            email.setMsg("???????????????" + userID.getName() + "???"
                         + "\n?????? ????????? ???????????? ????????? ????????? ??????????????????"
                         + "\nCODE : " + ePw);
            // ?????? ????????????
            try {
                out = response.getWriter();
                email.send();
                out.println("<script>alert('????????? ???????????????'); history.back();</script>");
                return new ResponseEntity<>(ePw, HttpStatus.OK);
            }
            catch (IOException e) {
                logger.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
        catch (EmailException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }*/

    @PostMapping("/user-findid")
    public ResponseEntity<?> sendEmail(
            @RequestBody
            SendEmailDto sendEmailDto) {
        logger.info("mail??? ?????????????????????.");
        logger.info("sendEmailDto : {}", sendEmailDto);

        try {
            //        User user = sendEmailService.findID(sendEmailDto.getName(), sendEmailDto.getEmail());
            User user = null;
            if (sendEmailDto.getUsername() == null) {
                user = sendEmailService.findID(sendEmailDto);
            }
            else {
                user = sendEmailService.findPassword(sendEmailDto);
            }

            logger.info("user : {}", user);

            sendEmailDto.setEmail(user.getEmail());
            sendEmailDto.setName(user.getName());

            // ?????? ??????
            sendEmailDto.setCode(sendEmailService.createKey());
            sendEmailDto.setUsername(user.getUsername());

            SimpleEmail email = new SimpleEmail();
            email.setHostName("smtp.naver.com");
            email.setSmtpPort(587);
            email.setAuthentication("littlegbear", "K1GLJB4ENX3M");

            // ???????????? SSL, TLS ?????? ??????
            email.setSSLOnConnect(true);
            email.setStartTLSEnabled(true);

            logger.info("mail??? ????????? ????????????.");
            // ????????? ?????? ?????? (SMTP ????????? ????????? ?????? ???????????? ???????????? ???????????? ??????!)
            email.setFrom("littlegbear@naver.com", "?????????", "utf-8");
            // ?????? ?????? ??????
//            email1.addTo("littlegbear@naver.com", "??????", "utf-8");
            email.addTo(sendEmailDto.getEmail(), sendEmailDto.getName(), "utf-8");
            // ?????? ??????
            email.setSubject("?????????/???????????? ?????? ?????? ??????");

            email.setMsg("??????????????? " + sendEmailDto.getName() + "???" + "\n?????? ????????? ?????????/???????????? ??????????????? ????????? ??????????????????" + "\nCODE : " +
                         sendEmailDto.getCode());
            // ?????? ????????????
            email.send();

            HttpHeaders messageResponse = new HttpHeaders();
            messageResponse.set("Response Message", "Email Send successfully!");


            // ????????? ????????? ?????? ????????????
//            return new ResponseEntity<>(ePw, messageResponse, HttpStatus.OK);
            return new ResponseEntity<>(sendEmailDto, messageResponse, HttpStatus.OK);
        }
        catch (Exception e) {
            logger.info("sendEmail : {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Error : Email Send Failure!"));
        }
    }

    // ???????????? ??????(????????????)
    @PutMapping("/user-findpassword")
    public ResponseEntity<?> findPassword(
            @RequestBody
            SignupRequest signUpRequest) {
        logger.info("signUpRequest {} ", signUpRequest);
        Optional<User> user = userService.findByUserName(signUpRequest.getUsername());

        logger.info("USER {} ", user);
        // user??? ???????????? ?????? ???
        // *!user.isPresent() => user == null
        if (!user.isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error : User Is Not Exist"));
        }

        try {
            // ??? ??????????????? ??????
            logger.info("test {} ", signUpRequest.getNewPassword());
            user.get().setPassword(encoder.encode(signUpRequest.getNewPassword()));
            logger.info("updateUser {} ", user);

            // ???????????? ??????
            sendEmailService.changePassword(user.get());
            return ResponseEntity.ok(new MessageResponse("Password change successfully!"));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error : Password change Failure!"));
        }
    }
}

