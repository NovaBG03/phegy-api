package tech.phegy.api.mapper.user;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.phegy.api.dto.user.response.UserInfoResponseDto;
import tech.phegy.api.model.user.PhegyRole;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.model.user.PhegyUser;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperImplTest {
    UserMapperImpl userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapperImpl();
    }

    @Test
    @DisplayName("Should map user to user info response dto")
    void shouldMapUserToUserInfoResponseDto() {
        String username = "ivan";
        String email = "ivan@abv.bg";
        PhegyRole userRole = PhegyRole.builder().level(PhegyRoleLevel.USER).build();
        Collection<PhegyRole> roles = Lists.newArrayList(userRole);
        PhegyUser user = PhegyUser.builder()
                .username(username)
                .email(email)
                .roles(roles)
                .build();

        UserInfoResponseDto actual = userMapper.phegyUserToUserInfoResponseDto(user);

        assertThat(actual)
                .matches(x -> x.getUsername().equals(username), "username is set")
                .matches(x -> x.getEmail().equals(email), "email is set")
                .matches(x -> x.getAuthorities().size() == 1, "set 1 authority");
    }

    @Test
    @DisplayName("Should map user to user info response dto when null")
    void shouldMapUserToUserInfoResponseDtoWhenNull() {
        UserInfoResponseDto actual = userMapper.phegyUserToUserInfoResponseDto(null);
        assertThat(actual).isNull();
    }
}
