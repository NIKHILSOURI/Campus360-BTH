# Campus360

## Introduction
Campus360 is an Android app that helps students and visitors navigate the university campus. It provides indoor floor maps, searchable rooms and points of interest, category browsing, and turn-by-turn visual guidance powered by an A* routing engine over a bidirectional graph.

TODO: Add one or more screenshots of the app, when you finalized the UI.  - To be upadated in future

## Architecture Overview

![Architecture](https://github.com/user-attachments/assets/8932ea39-3464-4ca6-b601-f77ac09c419c)



## User Stories

| **#** | **As a <type of user>** | **I want/need to <perform a task>**                                | **so that <achieve some goal>**                  | **Acceptance Criteria** |
| ----- | ----------------------- | ------------------------------------------------------------------ | ------------------------------------------------ | ----------------------- |
| US1   | As a user,              | I want to search for a lecture room by name or number,             | so that I can arrive on time without confusion.  | Search returns matching rooms; selecting a result opens details; empty/error states are shown. |
| US2   | As a user,              | I want to filter locations based on their type,                    | so that I have an overview of all meeting rooms. | Category list is browsable; selecting a category shows results; tapping an item opens details. |
| US3   | As a user,              | I want visual navigation instructions for a location,              | so that I know in which direction I have to go.  | A* route is computed; map displays a path; step-by-step guidance is visible during navigation. |
| US4   | As a user,              | I want to explore points of interest like the library or cafeteria | so that I can experience campus life.            | POIs are discoverable via search and categories; details show info and an option to start routing. |
| US5   | As a user,              | I want to choose my start location manually,                       | so that I can plan a route from any point.       | A start-location picker is available; chosen start is used to compute and render the route. |
| US6   | As a user,              | I want destination details before I start navigating,              | so that I can confirm I selected the right place.| Details screen shows name, type, description, and actions like “Show on Map” and “Route”. |
| US7   | As a user,              | I want an SOS button that shows the nearest exit,                  | so that I can evacuate safely and get help.      | SOS action is always accessible on home screen; after confirmation, the app highlights the nearest exit and renders a route from my current/selected start|
| US8   | As a user,              | I want clear feedback if no route is available,                    | so that I can adjust my plan.                    | If no path exists, a friendly message is shown and map/steps do not display an invalid route. |


### Development Status

Track the development status of the app.

Use
[ ] for not implemented
[x] for implemented.

[ ] US1

[ ] US2

[ ] US3

[ ] US4

[ ] US5

[ ] US6

[ ] US7

[ ] US8

## How to Use
  To be upadated in future
### Build

Using Gradle (from project root):
./gradlew clean assembleDebug
The APK is generated under `app/build/outputs/apk/debug/app-debug.apk`.

### Test

./gradlew test --rerun-tasks

### Run

Android Studio
- Open the project in Android Studio.
- Select a device/emulator and click Run.
  
## License

  To be upadated in future [Choose a License](https://choosealicense.com).
