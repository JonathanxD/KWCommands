# Command Parsing

Common processor of KWCommands uses a visit-like system to parse commands, this means that when the processor finds a command it will lookup for this command in the child list of the last parsed command.

Example, if you have a command structure like this:

`Command(open, sub=[Command(door), Command(window)]), Command(door, sub=[Command(open)])`

And a handler like this for each command: 

```kotlin 
Handler.create { print(it.command.fullname) }
```

Then `open door window` prints `open door` and `open window`, `open open` prints `open` and `open`, but `door open window` throws `CommandNotFoundException("window command (position 2 of [door, open, window]) cannot be found!")`. To solve this you need to use `&` operator: `door & open window` (prints `door` and `open window`).

Obs: If you want to pass `&` as argument of `door` uses the escape char (`\`): `door \& open window`.