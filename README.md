# SalahTimes

A command-line tool that prints daily Islamic prayer times for any city.

## Usage

Run the jar and enter your city when prompted:

```bash
java -jar target/SalahTimes-1.0-SNAPSHOT.jar
```

```
Welcome to the SalahTimes app
What city do you live in? Seattle
```

### Output

```
Prayer Schedule
---------------
Fajr       05:12:00
Dhuhr      12:21:00
Asr        15:47:00
Maghrib    18:00:00
Isha       19:31:00
```

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Building & Running

```bash
git clone https://github.com/yourusername/salahtimes.git
cd salahtimes
mvn clean package
java -jar target/SalahTimes-1.0-SNAPSHOT.jar
```

Or run directly without building a jar:

```bash
mvn clean compile exec:java
```

### Public Repository
https://github.com/Erik-Russell/SalahTimes

## License

MIT License — see [LICENSE](LICENSE) for details.
