# KWCommands

KWCommands is a complete rewrite of WCommands.

# Welcome 1.0

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

# Welcome 1.0 (in 1.1)

```java
@Cmd(description = "Welcome to KWCommands", requirements =
      @Require(
          subject = @Id(User.class),
          data = "repo.kwcommands.access",
          testerType = PermissionTester.class)
)
public void welcome(@Info Logger logger, @Info User user) {
    logger.log(Level.INFO, "Welcome {0} to KWCommands", user.getName());
}
```

# Welcome 1.1

```java
@CmdJson(type = CmdJsonType.RESOURCE, value = "/mycommands.json")
public class MyCommands {
    public void welcome(@Info Logger logger, @Info User user) {
        logger.log(Level.INFO, "Welcome {0} to KWCommands", user.getName());
    }
}
```

`mycommands.json`:

```json
{
  "name": "mycommands",
  "description": "My commands",
  "subCommands": ["welcome.json"]
}
```

`welcome.json`:
```json
{
  "name": "welcome",
  "description": "My commands",
  "handler": "method:welcome",
  "requiredInfo": [
    {
      "id": { "tags": ["logger"], "type": "Logger" }
    },
    {
      "id": { "tags": ["user"], "type": "User" }
    }
  ],
  "requirements": [
    {
      "info": { "tags": ["user"], "type": "User" },
      "tester": "com.my.PermissionTester",
      "data": "repo.kwcommands.access"
    }
  ]
}
```