# Campus360

## Introduction
Campus360 is an Android app that helps students and visitors navigate the university campus. It provides indoor floor maps, searchable rooms and points of interest, category browsing, and turn-by-turn visual guidance powered by an A* routing engine over a bidirectional graph.

TODO: Add one or more screenshots of the app, when you finalized the UI.

## Architecture Overview

TODO: Add simple diagram that explains the architecture.

## User Stories

| **#** | **As a <type of user>** | **I want/need to <perform a task>**                                | **so that <achieve some goal>**                  | **Acceptance Criteria** |
| ----- | ----------------------- | ------------------------------------------------------------------ | ------------------------------------------------ | ----------------------- |
| US1   | As a user,              | I want to search for a lecture room by name or number,             | so that I can arrive on time without confusion.  | Search returns matching rooms; selecting a result opens details; empty/error states are shown. |
| US2   | As a user,              | I want to filter locations based on their type,                    | so that I have an overview of all meeting rooms. | Category list is browsable; selecting a category shows results; tapping an item opens details. |
| US3   | As a user,              | I want visual navigation instructions for a location,              | so that I know in which direction I have to go.  | A* route is computed; map displays a path; step-by-step guidance is visible during navigation. |
| US4   | As a user,              | I want to explore points of interest like the library or cafeteria | so that I can experience campus life.            | POIs are discoverable via search and categories; details show info and an option to start routing. |
| US5   | As a user,              | I want to choose my start location manually,                       | so that I can plan a route from any point.       | A start-location picker is available; chosen start is used to compute and render the route. |
| US6   | As a user,              | I want destination details before I start navigating,              | so that I can confirm I selected the right place.| Details screen shows name, type, description, and actions like “Show on Map” and “Route”. |
| US7   | As a Swedish-speaking user, | I want the app in Swedish automatically,                      | so that I understand the interface.              | UI strings appear in Swedish when device locale is sv-SE; English otherwise; persists app-wide. |
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

### Build

TODO: Explain how the whole app can be build as an APK.

### Test

```sh
./gradlew test --rerun-tasks
```

### Run

TODO: Explain how to run the app on device or emulator.

## License

TODO: Add license and copyright notice. If you are not sure which license you should chose, have a look at [Choose a License](https://choosealicense.com).
