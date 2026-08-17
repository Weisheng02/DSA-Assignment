# TARUMT Resort Management System

Console-based Java application for the BMCS2063 Data Structures and Algorithms assignment.

## Requirements

- Java Development Kit (JDK) 8 or newer
- No external libraries or data files are required

## Run from a terminal

From the project root:

```sh
mkdir -p bin
javac -d bin $(find src -name '*.java')
java -cp bin App
```

The application starts at the main menu. Select one of the four modules and enter `0` to return to the preceding menu or exit the program.

## Modules

1. Walk-In Registrations and Standard Booking
2. Housekeeping and Task Log
3. Front-Desk Service
4. Loyalty and Rewards Service

## Project structure

- `src/adt` - Custom list, queue, stack and binary-search-tree implementations
- `src/entity` - Application entities
- `src/control` - Business logic
- `src/boundary` - Console user interfaces
- `src/App.java` - Application entry point
