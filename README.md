## Repository Note:
This repository was formed from work done for within a course directory. To have this simulator project listed indpependently I had a new repository created, so the full commit history is not visible.

# Android Simple Location Logger

## Description of App:

This app allows users to track their current location as well as see the coordinates of their recent locations. Upon opening the app, the user will be prompted to enable notifications, as later it will show when the service has started tracking.

When the user first presses the start tracking button, they will then be prompted to confirm or deny their location permissions.Once approved they can press the button again to begin tracking, with a notification message and icon showing when the service is active. 

Their current location will be shown in the two textViews at the top of the screen display for both longitude and latitude. At the bottom of the screen, up to the last five recent locations that have been read will be displayed. Once more than five locations have been read, the oldest location will be removed and replaced with the newest one. The user can use the stop tracking button to disable tracking at the moment.

If the user closes the app, they can reopen it later to see previously saved location data, if it’s available.

## Design Choices:

The use of TextViews were prioritized as they could receive intents to show and update the location values, as well as represent text data that could be saved to internal storage. 

Only two buttons were used, as starting and stopping the location tracking was sufficient for the program. It allowed permissions to initially be checked, locations to be updated like the current reviews and the log over time, and it allowed for the user to choose to end tracking when they are finished. 

The app uses a Foreground Service to handle the logic of collecting the location data, as is because these types of services provide work intended to be visible to the user, so I also used a persistent notification to clearly disclose when it is in use.

The app saves recent locations using internal storage. This is because it's convenient for storing simple information like past location data in logs, and it allows saved data to be reloaded the next time the user opens the app, like the textView of indecent locations, as well as the program’s list data structure.
