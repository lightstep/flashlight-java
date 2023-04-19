# Flashlight

Flashlight is a command line tool that scans the provided classpath
and identifies 'interesting' methods to instrument with OpenTelemetry.

## Installation

Download the [latest release](https://github.com/lightstep/flashlight-java/releases/latest/download/flashlight.jar).

## Usage

Gradle:

```shell
java -jar flashlight.jar **/build/classes/java/main/
```

Maven:

```shell
java -jar flashlight.jar **/target/classes/
```

All options:

```shell
java -jar flashlight.jar [-hV] [-b=NUMBER] [-c=NUMBER] <paths>...
```

```
      <paths>...        the folders or jar files to scan
  -b, --branch=NUMBER   number of branch (if/while/for) instructions considered
                          interesting (default: 5)
  -c, --call=NUMBER     number of method call instructions considered
                          interesting (default: 10)
  -h, --help            Show this help message and exit.
  -V, --version         Print version information and exit.
```

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

Note: If there is sufficient interest, this project can be donated to OpenTelemetry.

## License

[Apache 2.0](https://choosealicense.com/licenses/apache-2.0/)
