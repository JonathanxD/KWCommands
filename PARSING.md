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

# Argument parsing

In KWCommands there is no argument ordering system (with exception of reflection), this is a design choice, when KWCommands Parser finds a command with arguments, it starts looking up for arguments, the parser will use the first `Argument` that the `Matcher` returns `true` for the input string to create an `ArgumentContainer`, this means that if you have following command:

```
command {
    stringName { "example" }
    arguments {
        +intArg {
            id { "value" }
            name { "value" }
        }
        +stringArg {
            id { "name" }
            name { "name" }
        }
    }
    handlerWithContext {
        val value: Int = it.getArg("value")
        val name: String = it.getArg("name")

        println("Int = $value. String = $name")
    }
}

``` 

And call it with this string list: `listOf("example", "--recursive", "x")`, it will print `Int = 9. String = Hello`, the order of arguments only matters when there is more than one argument of same type. You can also use named arguments, introduced in (`1.1`), this ensures correctness, have special exceptions (example, when you input a string for a int argument). You can also enable `KWParserOptions.ORDER` option (introduced in `1.1`).