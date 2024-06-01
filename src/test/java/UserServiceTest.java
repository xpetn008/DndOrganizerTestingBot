import org.example.Main;
import org.example.models.exceptions.BadDataException;
import org.example.models.exceptions.UserAlreadyRegisteredException;
import org.example.models.exceptions.UserIsNotRegisteredException;
import org.example.models.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.User;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = Main.class)
public class UserServiceTest {
    @Autowired
    private UserService userService;
    @Test
    public void nicknameIsUsed(){
        String nickname = "Хуёва";
        boolean expected = true;
        boolean actual = userService.nicknameIsUsed(nickname);
        Assert.isTrue(expected = actual, "Test passed");
    }
    @Test
    public void create() throws UserAlreadyRegisteredException, UserIsNotRegisteredException, BadDataException {
        User user = new User();
        boolean master = true;
        user.setUserName("TestingUserName");
        user.setId(Long.parseLong("00000000"));
        userService.create(user);
        Assert.isTrue(userService.isRegistered(user), "Test create is passed.");
        if (userService.isRegistered(user)){
            userService.delete(user);
        }

    }
}
