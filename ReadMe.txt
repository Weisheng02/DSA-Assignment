TARUMT Resort Management System
================================

Requirements
------------
Java Development Kit (JDK) 8 or newer. No external libraries or data files are required.

How to run
----------
1. Open this folder as a Java project in NetBeans, or open a terminal in this folder.
2. Compile all Java files under src.
3. Run the App class.

Terminal commands:

  mkdir -p bin
  javac -d bin $(find src -name '*.java')
  java -cp bin App

The program displays a main menu with these modules:
1. Walk-In Registrations and Standard Booking
2. Housekeeping and Task Log
3. Front-Desk Service
4. Loyalty and Rewards Service

Enter 0 in a menu to return to the previous menu or exit the application.

Source-code layout
------------------
src/adt       Custom collection ADTs
src/entity    Application entities
src/control   Business logic
src/boundary  Console UI classes
src/App.java  Main application entry point
