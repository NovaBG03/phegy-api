package tech.phegy.api.mapper.user;

import org.springframework.stereotype.Component;
import tech.phegy.api.dto.user.response.UserInfoResponseDto;
import tech.phegy.api.model.user.PhegyUser;

import java.util.stream.Collectors;

/**
 * User mapper.
 *
 * @author Nikita
 */
@Component
public class UserMapperImpl implements UserMapper {
    @Override
    public UserInfoResponseDto phegyUserToUserInfoResponseDto(PhegyUser user) {
        if (user == null) {
            return null;
        }

        return UserInfoResponseDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .authorities(user.getRoles()
                        .stream()
                        .flatMap(role -> role.getLevel().getAuthorities().stream())
                        .collect(Collectors.toSet()))
                .build();
    }
}
