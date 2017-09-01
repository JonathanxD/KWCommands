# KWCommands

KWCommands is a complete rewrite of WCommands.

# Welcome

```java
@Cmd(description = "Welcome to KWCommands", requirements = 
      @Require(
            subject = @Id(User.class), 
            data = "repo.kwcommands.access", 
            infoType = Permission.class,
            testerType = PermissionTester.class)
)
public void welcome(@Info Logger logger, @Info User user) {
    logger.log(Level.INFO, "Welcome {0} to KWCommands", user.getName());
}
```