# Daikin Air Conditioner Control App - Architecture Documentation

## Overview

This Android application provides remote control capabilities for Daikin air conditioning systems through their SkyFi interface. The app allows users to control AC settings, manage zone configurations, and schedule automated operations.

## App Goals

1. **Remote AC Control**: Control power, temperature, mode, and fan settings
2. **Zone Management**: Enable/disable individual AC zones
3. **Scheduling**: Create and manage automated AC schedules with start/end times
4. **Offline Capability**: Mock mode for testing when AC unit is unavailable
5. **User-Friendly Interface**: Modern Material 3 design with intuitive controls

## Technical Architecture

### Technology Stack
- **Platform**: Android (API 26+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Networking**: Ktor Client
- **Database**: Room (SQLite)
- **Background Tasks**: WorkManager
- **Dependency Injection**: Manual DI via NetworkModule

### Project Structure

```
app/src/main/java/com/example/airconapp/
├── MainActivity.kt                 # Main activity with navigation
├── SplashActivity.kt              # Splash screen
├── data/
│   ├── AirconApi.kt              # API service and data models
│   ├── ConnectionException.kt     # Custom exception handling
│   ├── db/                       # Database layer
│   │   ├── AppDatabase.kt        # Room database configuration
│   │   ├── SchedulerProfile.kt   # Schedule entity and converters
│   │   └── SchedulerProfileDao.kt # Database access object
│   ├── mock/
│   │   └── MockAirconApiService.kt # Mock API for testing
│   └── repo/
│       └── SchedulerRepository.kt # Repository pattern implementation
├── di/
│   └── NetworkModule.kt          # Dependency injection setup
├── presentation/
│   ├── MainViewModel.kt          # Main screen view model
│   ├── SchedulerViewModel.kt     # Schedule management view model
│   └── SettingsViewModel.kt      # Settings view model
├── scheduler/
│   └── ScheduleManager.kt        # Schedule execution management
├── ui/
│   ├── components/
│   │   ├── AirconControlScreenContent.kt # Main control UI
│   │   └── ConnectionErrorDialog.kt      # Error dialog
│   ├── screens/
│   │   ├── AddEditScheduleScreen.kt      # Schedule editor
│   │   ├── AirconControlScreen.kt        # Main control screen
│   │   ├── SchedulerScreen.kt            # Schedule list
│   │   └── SettingsScreen.kt             # App settings
│   └── theme/                    # Material 3 theming
├── worker/
│   ├── EndTimeWorker.kt          # Schedule end time handler
│   └── ScheduleWorker.kt         # Schedule start time handler
```

## Core Components

### 1. Data Layer

#### API Service (`AirconApi.kt`)
- **Purpose**: Communicates with Daikin SkyFi API
- **Key Models**:
  - `ControlInfo`: AC power, mode, temperature, fan settings
  - `Zone`: Individual zone name and on/off status
- **Endpoints**:
  - `GET /skyfi/aircon/get_control_info`: Retrieve AC status
  - `POST /skyfi/aircon/set_control_info`: Update AC settings
  - `GET /skyfi/aircon/get_zone_setting`: Get zone configurations
  - `POST /skyfi/aircon/set_zone_setting`: Update zone settings

#### Database (`data/db/`)
- **Purpose**: Persist schedule configurations locally
- **Entity**: `SchedulerProfile` with Room ORM
- **Features**: Type converters for complex objects (ControlInfo, Zone lists)

### 2. Presentation Layer

#### ViewModels
- **MainViewModel**: Manages real-time AC control and status
  - Implements optimistic updates for responsive UI
  - Handles periodic status refresh (5-second intervals)
  - Manages connection error states and mock mode switching
- **SchedulerViewModel**: Handles schedule CRUD operations
- **SettingsViewModel**: Manages app configuration

#### UI Components
- Built with Jetpack Compose and Material 3
- Navigation handled by Navigation Compose
- Screens include main control, scheduler, settings, and schedule editor

### 3. Scheduling System

#### ScheduleManager (`scheduler/ScheduleManager.kt`)
- **Purpose**: Manages automated schedule execution
- **Features**:
  - Uses WorkManager for reliable background execution
  - Supports start and end time scheduling
  - Handles day transitions for overnight schedules
  - Provides unique work policies to prevent duplicates

#### Workers (`worker/`)
- **ScheduleWorker**: Executes scheduled AC configuration changes
- **EndTimeWorker**: Handles schedule termination logic
- Both require network connectivity constraints

### 4. Dependency Injection

#### NetworkModule (`di/NetworkModule.kt`)
- **Purpose**: Provides singleton instances and configuration
- **Features**:
  - Runtime switching between real and mock API services
  - Centralized HTTP client configuration
  - Repository pattern implementation

## Key Features

### 1. Real-time Control
- Live AC status monitoring with 5-second refresh intervals
- Optimistic UI updates for immediate feedback
- Graceful error handling with connection retry options

### 2. Zone Management
- Support for multiple AC zones (typically 8 zones)
- Individual zone control with name persistence
- Dynamic zone discovery from AC unit

### 3. Schedule Management
- Time-based automation with start/end times
- Persistent storage in local database
- Background execution via WorkManager
- Support for complex scheduling patterns (overnight schedules)

### 4. Offline Mode
- Mock API service for development and testing
- Fallback mode when AC unit is unreachable
- Seamless switching between real and mock modes

## API Integration

The app integrates with Daikin's SkyFi API using a custom HTTP interface:

### Data Format
- **Control Info**: `"ret=OK,pow=1,mode=2,stemp=24,f_rate=5,f_dir=0"`
- **Zone Settings**: `"zone_onoff=1;0;0;0;0;0;0;0,zone_name=Zone1;Zone2;..."`

### Connection Handling
- Network timeouts handled with custom `ConnectionException`
- Automatic fallback to mock mode on connection failures
- User notification for connection issues with recovery options

## Security Considerations

- Local network communication only (no external API dependencies)
- No sensitive data storage or transmission
- INTERNET permission for local network AC communication
- Database encryption not implemented (schedules contain no sensitive data)

## Future Enhancement Opportunities

1. **User Authentication**: Multi-user support with profiles
2. **Advanced Scheduling**: Weekly patterns, seasonal adjustments
3. **Energy Monitoring**: Usage tracking and efficiency reporting
4. **Smart Features**: Geo-fencing, weather integration
5. **UI Improvements**: Charts, historical data visualization
6. **Cloud Sync**: Schedule backup and multi-device synchronization

## Development Notes

- Minimum SDK: API 26 (Android 8.0)
- Target SDK: API 34 (Android 14)
- Kotlin compiler: JVM target 1.8
- Build system: Gradle with Kotlin DSL
- Testing: JUnit, MockK, Espresso support configured