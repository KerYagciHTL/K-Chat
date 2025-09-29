# K-Chat - Real-time Messenger Application

A functional messenger application built with JavaFX and WebSocket technology for real-time messaging.

## Architecture

The application follows a clean, modular architecture:

```
src/main/java/kchat/
├── MessengerApp.java           # Main JavaFX application (GUI + Server)
├── ServerLauncher.java         # Standalone server launcher (Server only)
├── client/
│   └── MessengerClient.java    # WebSocket client
├── server/
│   └── MessengerServer.java    # WebSocket server with broadcasting
├── model/
│   └── Message.java            # Message data model with JSON support
└── ui/
    └── MessengerWindow.java     # Main UI components
```

## Features

- Real-time messaging using WebSocket connections
- Multiple clients can connect simultaneously
- Auto-broadcast messages to all connected users
- Simple, clean JavaFX UI
- Username customization
- Connection status indicators
- Auto-scrolling message area
- JSON message serialization
- Comprehensive test coverage

## Running the Application

### Prerequisites
- Java 17+ installed
- Gradle (included via wrapper)

### Three Different Ways to Run:

#### Option 1: Complete Application (GUI Client + Server)
```bash
./gradlew run
```
**What this does:**
- Starts the MessengerServer on port 8080
- Launches the JavaFX GUI client application
- Connects the GUI client to the server automatically
- Perfect for single-user testing or when you want both server and client together

#### Option 2: Standalone Server Only
```bash
./gradlew runServer
```
**What this does:**
- Starts ONLY the MessengerServer on port 8080 (no GUI)
- Runs in console mode with minimal output
- Waits for Enter key to stop the server
- Perfect for dedicated server hosting when you want to run multiple separate clients

#### Option 3: Client Only (No Server)
```bash
./gradlew runClient
```
**What this does:**
- Launches ONLY the JavaFX GUI client application (no server)
- Connects to an existing server (must be running separately)
- Perfect for connecting multiple clients to a dedicated server

### Key Differences Between the Commands:

| Command | Server | GUI Client | Use Case |
|---------|--------|------------|----------|
| `./gradlew run` | ✅ Starts | ✅ Starts | Development, single-user testing |
| `./gradlew runServer` | ✅ Starts | ❌ None | Dedicated server hosting |  
| `./gradlew runClient` | ❌ None | ✅ Starts | Connect to existing server |

### Testing with Multiple Clients
1. **Start dedicated server**: `./gradlew runServer`
2. **Run multiple clients**: Open new terminals and run `./gradlew runClient` in each
3. **Change usernames** in each client window
4. **Start messaging** between clients

## Testing

The application includes comprehensive test coverage:

### Running Tests
```bash
./gradlew test
```

### Test Structure
```
src/test/java/kchat/
├── MessengerIntegrationTest.java    # Integration tests
├── model/
│   └── MessageTest.java            # Message model tests
└── server/
    └── MessengerServerTest.java    # Server functionality tests
```

### Test Coverage Includes:
- **Message Model**: JSON serialization/deserialization, getters/setters, validation
- **Server Functionality**: Server lifecycle, connection management, port configuration
- **Integration Testing**: End-to-end server operations, multiple instances

## Usage

1. **Connect**: The application auto-connects to localhost:8080
2. **Set Username**: Enter your desired username before connecting
3. **Send Messages**: Type in the message field and press Enter or click Send
4. **View Messages**: All messages appear in the scrollable message area with timestamps

## Technical Details

- **Server**: Java-WebSocket library for WebSocket server implementation
- **Client**: WebSocket client with automatic reconnection capability
- **UI**: JavaFX for clean, native desktop interface
- **Messaging**: JSON serialization using Jackson
- **Architecture**: Clean separation of concerns with dedicated packages
- **Package Structure**: Organized under `kchat` namespace
- **Testing**: JUnit 5 with comprehensive coverage

## Development

### Building
```bash
./gradlew build
```

### Running Tests
```bash
./gradlew test
```

### Clean Build
```bash
./gradlew clean build
```

## No Security Features
This is a simple demonstration application with no authentication, encryption, or input validation - suitable for testing and learning purposes only.
