package tech.phegy.api.dto.user.response;

import com.google.common.collect.Sets;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponseDto {
    private String username;
    private String email;
    @Builder.Default
    private Set<? extends GrantedAuthority> authorities = Sets.newHashSet();
}
