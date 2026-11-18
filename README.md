# Wilson_Stuart_S2323311 – Mobile Platform Development coursework 2026/27

Android app (Java/XML) showing Traffic Scotland unplanned events (incidents) and current roadworks.
It uses a single Activity with Fragments, organised with MVVM (ViewModel + LiveData).

## Building from source

The API keys are not in this repository (they are in `local.properties`, which Git ignores).
To build the project with live data, add these three lines to `local.properties`:

```
MAPS_API_KEY=your_google_maps_key
TS_CLIENT_ID=your_traffic_scotland_client_id
TS_CLIENT_KEY=your_traffic_scotland_client_key
```

Without them the project still builds using the placeholder values in `local.defaults.properties`.
The ready-built `.apk` in the GitHub release already contains working keys.

Contains Traffic Scotland data. Icons: Material Icons by Google (Apache License 2.0).
