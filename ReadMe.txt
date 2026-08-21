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
  javac -encoding UTF-8 -d bin $(find src -name '*.java')
  java -cp bin App

The program displays a main menu with these modules:
1. Walk-In Registrations and Standard Booking
2. Housekeeping and Task Log
3. Front-Desk Service
4. Loyalty and Rewards Service

Enter 0 in a menu to return to the previous menu or exit the application.
Invalid menu/input values display "Wrong input" and can be entered again.
After an operation result is displayed, press Enter to return to its previous menu.

Source-code layout
------------------
src/adt       Custom collection ADTs
src/entity    Application entities
src/control   Business logic
src/boundary  Console UI classes
src/App.java  Main application entry point

Design note
-----------
The console boundaries follow the same simple style as the supplied ECBDemo:
the boundary may receive and display entity objects through their getters, while
the controller owns the business rules and updates the shared entities.  The
controllers share one in-memory room, guest, and booking collection when the
application is running, so a status or booking update is visible to every
module.  Housekeeping reports use display strings where a report only needs
formatted output; they do not create duplicate Room view classes.

Regression checks
-----------------
After compiling the source, compile and run the checks with assertions enabled:

  javac -encoding UTF-8 -cp bin -d bin test/ControllerRegressionTest.java
  java -ea -cp bin ControllerRegressionTest

The checks cover shared room/booking state, date and overlap validation,
BST duplicate rejection and balancing, housekeeping rollback safety,
check-in/check-out state consistency, repeat-stay loyalty synchronization,
PointBatch consumption/expiry, and reward settlement/report counts.
