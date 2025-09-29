# Simple Messenger Application

A functional messenger application built with JavaFX and WebSocket technology for real-time messaging.

## Architecture

The application follows a clean, modular architecture:

```
src/main/java/org/example/
├── MessengerApp.java           # Main JavaFX application
├── ServerLauncher.java         # Standalone server launcher
├── client/
│   └── MessengerClient.java    # WebSocket client
├── server/
│   └── MessengerServer.java    # WebSocket server with broadcasting
├── model/
│   └── Message.java            # Message data model
└── ui/
    └── MessengerWindow.java     # Main UI components
```

## Features

- Real-time messaging using WebSocket connections
- Multiple clients can connect simultaneously
- Auto-broadcast messages to all connected users
- Simple, clean UI with no unnecessary complexity
- Username customization
- Connection status indicators
- Auto-scrolling message area

## Running the Application

### Prerequisites
Make sure you have Java 17+ installed.

### Option 1: Run Complete Application (Client + Server)
```bash
./gradlew run
```
This starts both the server and client in one application.

### Option 2: Run Standalone Server
```bash
./gradlew run --args="kchat.ServerLauncher"
```
Then run clients separately by running the main application.

### Testing with Multiple Clients
1. Start the application: `./gradlew run`
2. Open additional client instances by running the application again in new terminals
3. Change usernames in each client
4. Start messaging between clients

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

## No Security Features
This is a simple demonstration application with no authentication, encryption, or input validation - suitable for testing and learning purposes only.
