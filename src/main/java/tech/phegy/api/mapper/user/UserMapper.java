package tech.phegy.api.mapper.user;

import tech.phegy.api.dto.user.response.UserInfoResponseDto;
import tech.phegy.api.model.user.PhegyUser;

/**
 * User mapper.
 *
 * @author Nikita
 */
public interface UserMapper {
    UserInfoResponseDto phegyUserToUserInfoResponseDto(PhegyUser user);
}
