package tech.phegy.api.bootstrap;

import com.google.common.collect.Lists;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tech.phegy.api.model.points.PointsBag;
import tech.phegy.api.repository.PhegyRoleRepository;
import tech.phegy.api.repository.PhegyUserRepository;
import tech.phegy.api.model.user.PhegyRole;
import tech.phegy.api.model.user.PhegyRoleLevel;
import tech.phegy.api.model.user.PhegyUser;
import tech.phegy.api.repository.PointsBagRepository;
import tech.phegy.api.service.imageGenerator.ImageGeneratorService;
import tech.phegy.api.service.storage.CloudStorageService;
import tech.phegy.api.service.storage.StoragePath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
@Profile("dev")
public class Bootstrap implements CommandLineRunner {
    private final PasswordEncoder passwordEncoder;
    private final PhegyUserRepository userRepository;
    private final PhegyRoleRepository roleRepository;
    private final ImageGeneratorService imageGeneratorService;
    private final CloudStorageService cloudStorageService;
    private final PointsBagRepository pointsBagRepository;

    public Bootstrap(PasswordEncoder passwordEncoder,
                     PhegyUserRepository userRepository,
                     PhegyRoleRepository roleRepository,
                     ImageGeneratorService imageGeneratorService,
                     CloudStorageService cloudStorageService,
                     PointsBagRepository pointsBagRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.imageGeneratorService = imageGeneratorService;
        this.cloudStorageService = cloudStorageService;
        this.pointsBagRepository = pointsBagRepository;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            loadRoles();
        }

        if (userRepository.count() == 0) {
            loadUsers();
        }
    }

    private void loadRoles() {
        Iterable<PhegyRole> roles = Arrays.stream(PhegyRoleLevel.values())
                .map(roleLevel -> PhegyRole.builder().level(roleLevel).build())
                .collect(Collectors.toList());

        roleRepository.saveAll(roles);
    }

    private void loadUsers() {
        Collection<PhegyUser> users = new ArrayList<>();
        PhegyRole userRole = this.roleRepository.getByLevel(PhegyRoleLevel.USER);
        PhegyRole moderatorRole = this.roleRepository.getByLevel(PhegyRoleLevel.MODERATOR);
        PhegyRole adminRole = this.roleRepository.getByLevel(PhegyRoleLevel.ADMIN);

        PhegyUser user = PhegyUser.builder()
                .username("ivan")
                .email("ivan@abv.bg")
                .encodedPassword(passwordEncoder.encode("Ivan123"))
                .build();
        user.addRole(userRole);
        userRepository.save(user);
        users.add(user);

        PhegyUser moderator = PhegyUser.builder()
                .username("moderen")
                .email("mod@abv.bg")
                .encodedPassword(passwordEncoder.encode("Moderen123"))
                .build();
        moderator.addRoles(Lists.newArrayList(userRole, moderatorRole));
        userRepository.save(moderator);
        users.add(moderator);

        PhegyUser admin = PhegyUser.builder()
                .username("admin")
                .email("admin@abv.bg")
                .encodedPassword(passwordEncoder.encode("Admin123"))
                .build();
        admin.addRoles(Lists.newArrayList(userRole, moderatorRole, adminRole));
        userRepository.save(admin);
        users.add(admin);

        users.forEach(u -> {
            setImage(u);
            createStatistics(u);
        });
    }

    private void createStatistics(PhegyUser user) {
        PointsBag pointsBag = PointsBag.builder()
                .points(100000d)
                .user(user)
                .build();

        pointsBagRepository.save(pointsBag);
    }

    private void setImage(PhegyUser user) {
        try {
            cloudStorageService.upload(
                    imageGeneratorService.generateProfilePic(user.getUsername()),
                    user.getUsername() + ".png",
                    StoragePath.USER);
        } catch (Exception ignored) {
        }
    }
}
