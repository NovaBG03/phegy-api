package tech.phegy.api.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.phegy.api.dto.user.request.EmailDto;
import tech.phegy.api.dto.user.request.PasswordDto;
import tech.phegy.api.dto.user.request.UserRegisterDto;
import tech.phegy.api.dto.user.response.AchievementsListResponseDto;
import tech.phegy.api.dto.user.response.RequestActivationResponseDto;
import tech.phegy.api.dto.user.response.UserInfoResponseDto;
import tech.phegy.api.service.jwt.RefreshTokenService;
import tech.phegy.api.service.register.RegisterService;
import tech.phegy.api.mapper.user.UserMapper;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.service.AchievementsService;
import tech.phegy.api.service.PhegyUserService;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

/**
 * User controller.
 *
 * @author Nikita.
 */
@RestController
@RequestMapping("/api/v1")
public class PhegyUserController {
    private final RefreshTokenService refreshTokenService;
    private final RegisterService registerService;
    private final PhegyUserService userService;
    private final AchievementsService achievementsService;
    private final UserMapper userMapper;

    /**
     * Constructs new instance with needed dependencies.
     */
    public PhegyUserController(RefreshTokenService refreshTokenService,
                               RegisterService registerService,
                               PhegyUserService userService,
                               AchievementsService achievementsService,
                               UserMapper userMapper) {
        this.refreshTokenService = refreshTokenService;
        this.registerService = registerService;
        this.userService = userService;
        this.achievementsService = achievementsService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public UserInfoResponseDto getPrincipalInfo(Principal principal) {
        final PhegyUser user = userService.getUserByUsername(principal.getName());
        return this.userMapper.phegyUserToUserInfoResponseDto(user);
    }

    @GetMapping("/achievements/{username}")
    public AchievementsListResponseDto getAchievements(@PathVariable String username) {
        return this.achievementsService.getAchievements(username);
    }

    @PostMapping("/register")
    public void register(@RequestBody UserRegisterDto userDto) {
        this.registerService.registerUser(
                userDto.getUsername().trim(),
                userDto.getEmail().trim(),
                userDto.getPassword()
        );
    }

    @PostMapping("/requestActivation")
    public RequestActivationResponseDto requestActivation(Principal principal) {
        return new RequestActivationResponseDto(
                this.registerService.resendActivationLink(principal.getName()));
    }

    @PostMapping("/activate/{token}")
    public void activate(@PathVariable String token) {
        this.registerService.activateUser(token);
    }

    @PostMapping("/refresh/{token}")
    public void refresh(HttpServletResponse response, @PathVariable String token) {
        this.refreshTokenService.refreshAccess(response, token);
    }

    @PostMapping("/me/email")
    public void changeEmail(@RequestBody EmailDto emailDto, Principal principal) {
        this.userService.changeUserEmail(emailDto.getEmail(), principal.getName());
    }

    @PostMapping("/me/password")
    public void changePassword(@RequestBody PasswordDto passwordDto, Principal principal) {
        this.userService.changePassword(passwordDto.getOldPassword(),
                passwordDto.getNewPassword(),
                passwordDto.getConfirmPassword(),
                principal.getName());
    }

    @PostMapping("/me/image")
    public void updateProfilePic(@RequestParam MultipartFile image, Principal principal) {
        this.userService.setProfileImage(image, principal.getName());
    }
}
