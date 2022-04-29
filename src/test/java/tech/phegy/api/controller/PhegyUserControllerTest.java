package tech.phegy.api.controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tech.phegy.api.dto.user.request.EmailDto;
import tech.phegy.api.service.jwt.RefreshTokenService;
import tech.phegy.api.dto.user.request.PasswordDto;
import tech.phegy.api.dto.user.request.UserRegisterDto;
import tech.phegy.api.dto.user.response.AchievementsListResponseDto;
import tech.phegy.api.dto.user.response.UserInfoResponseDto;
import tech.phegy.api.mapper.user.UserMapper;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.service.AchievementsService;
import tech.phegy.api.service.PhegyUserService;
import tech.phegy.api.service.register.RegisterService;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.phegy.api.TestUtils.json;

@SpringBootTest
class PhegyUserControllerTest {
    @MockBean
    RefreshTokenService refreshTokenService;
    @MockBean
    RegisterService registerService;
    @MockBean
    PhegyUserService userService;
    @MockBean
    AchievementsService achievementsService;
    @MockBean
    UserMapper userMapper;

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Should get principal info")
    void shouldGetPrincipalInfo() throws Exception {
        String username = "username";
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn(username);

        PhegyUser user = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .build();
        UserInfoResponseDto responseDto = UserInfoResponseDto.builder().build();

        when(userService.getUserByUsername(username)).thenReturn(user);
        when(userMapper.phegyUserToUserInfoResponseDto(user)).thenReturn(responseDto);

        mvc.perform(get("/api/v1/me").with(user(username)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should throw exception when get principal info user not found")
    void shouldThrowExceptionWhenGetPrincipalInfoUserNotFound() throws Exception {
        String username = "username";
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn(username);

        PhegyUser user = PhegyUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .build();
        UserInfoResponseDto responseDto = UserInfoResponseDto.builder().build();

        when(userService.getUserByUsername(username)).thenThrow(UsernameNotFoundException.class);
        when(userMapper.phegyUserToUserInfoResponseDto(user)).thenReturn(responseDto);

        mvc.perform(get("/api/v1/me").with(user(username)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get user achievements without principal")
    void shouldGetUserAchievementsWithoutPrincipal() throws Exception {
        String username = "ivan";
        AchievementsListResponseDto responseDto = AchievementsListResponseDto.builder().build();

        when(achievementsService.getAchievements(username)).thenReturn(responseDto);

        mvc.perform(get("/api/v1/achievements/" + username))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should register new user")
    void shouldRegisterNewUser() throws Exception {
        String username = "ivan";
        String email = "ivan@abv.bg";
        String password = "Sup3rS3cur3P@as";
        UserRegisterDto userRegisterDto = UserRegisterDto.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();

        mvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(userRegisterDto)))
                .andExpect(status().isOk());

        verify(registerService).registerUser(username, email, password);
    }

    @Test
    @DisplayName("Should request activation link for principal")
    void shouldRequestActivationLinkForPrincipal() throws Exception {
        String username = "ivan";
        Long minDelayInSeconds = 3000L;

        when(registerService.resendActivationLink(username)).thenReturn(minDelayInSeconds);

        mvc.perform(post("/api/v1/requestActivation").with(user(username)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should not allow request activation link without principal")
    void shouldNotAllowRequestActivationLinkWithoutPrincipal() throws Exception {
        mvc.perform(post("/api/v1/requestActivation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should activate user without principal")
    void shouldActivateUserWithoutPrincipal() throws Exception {
        String token = "token";

        mvc.perform(post("/api/v1/activate/" + token))
                .andExpect(status().isOk());

        verify(registerService).activateUser(token);
    }

    @Test
    @DisplayName("Should refresh access without principal")
    void shouldRefreshAccessWithoutPrincipal() throws Exception {
        String token = "token";

        mvc.perform(post("/api/v1/refresh/" + token))
                .andExpect(status().isOk());

        ArgumentCaptor<String> tokenArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenService).refreshAccess(any(), tokenArgumentCaptor.capture());
        assertThat(tokenArgumentCaptor.getValue()).isEqualTo(token);
    }

    @Test
    @DisplayName("Should change email")
    void shouldChangeEmail() throws Exception {
        String username = "token";
        String newEmail = "new@mail.bg";
        EmailDto emailDto = EmailDto.builder().email(newEmail).build();

        mvc.perform(post("/api/v1/me/email")
                        .with(user(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(emailDto)))
                .andExpect(status().isOk());

        verify(userService).changeUserEmail(newEmail, username);
    }

    @Test
    @DisplayName("Should change password")
    void shouldChangePassword() throws Exception {
        String username = "ivan";
        String oldPassword = "oldPass";
        String newPassword = "newPass";
        String confirmPassword = "confirmPass";
        PasswordDto passwordDto = PasswordDto.builder()
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .confirmPassword(confirmPassword)
                .build();

        mvc.perform(post("/api/v1/me/password")
                        .with(user(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(passwordDto)))
                .andExpect(status().isOk());

        verify(userService).changePassword(oldPassword, newPassword, confirmPassword, username);
    }

    @Test
    @DisplayName("Should upload profile picture")
    void shouldUploadProfilePicture() throws Exception {
        String username = "ivan";
        MockMultipartFile image = new MockMultipartFile("image", new byte[0]);

        mvc.perform(multipart("/api/v1/me/image")
                        .file(image)
                        .with(user(username)))
                .andExpect(status().isOk());

        verify(userService).setProfileImage(image, username);
    }
}