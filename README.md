# K-Chat - Real-time Messenger Application

A functional messenger application built with JavaFX and WebSocket technology for real-time messaging.

## Architecture

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
- Optional shared-secret end-to-end style message encryption (experimental)
- Optional TLS (wss://) transport security
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
4. **(Optional)** Enter a shared Secret in all windows for encrypted messages
5. **Start messaging** between clients

## Security Features (Optional / Experimental)

K-Chat now offers two optional safety layers you can enable to reduce the chance of someone spying on your messages:

### 1. Transport Layer Security (TLS) - wss://
Encrypts the WebSocket transport (protects against basic network sniffing). Requires a Java KeyStore.

Enable by passing system properties when launching the JVM:
```bash
# Generate a self-signed certificate (example):
keytool -genkeypair -alias kchat -keyalg RSA -keysize 2048 -validity 365 \
  -keystore kchat.jks -storepass changeit -keypass changeit \
  -dname "CN=localhost, OU=Dev, O=KChat, L=Local, S=NA, C=US"

# Run server with TLS
env JAVA_TOOL_OPTIONS="-Dkchat.ssl=true -Dkchat.keystore=./kchat.jks -Dkchat.keystorePassword=changeit" \
  ./gradlew runServer

# Run client with TLS (same properties so it trusts the self-signed cert)
env JAVA_TOOL_OPTIONS="-Dkchat.ssl=true -Dkchat.keystore=./kchat.jks -Dkchat.keystorePassword=changeit" \
  ./gradlew runClient
```
When enabled the client automatically switches from `ws://` to `wss://`.

### 2. Shared-Secret Message Encryption (Application Layer)
Adds an additional encryption layer on the message contents before they leave the client. The server simply relays the encrypted payload and cannot read it (assuming it does not know the secret). All participants must enter the *same* Secret in the UI (top panel) before connecting. Encrypted messages appear with status "Connected (Encrypted)".

How it works (simplified):
- Derives an AES-256 key from the secret using PBKDF2 (fixed salt, 65k iterations)
- Encrypts each message with AES/GCM + random 96-bit IV
- Encodes as `ENC:<Base64>` inside the JSON `content` field
- Recipients with the same secret decrypt transparently

If a client doesn't have the secret, it will just see the `ENC:...` text.

### Important Limitations / Disclaimers
- This is NOT production-grade end-to-end encryption
- Fixed salt, no forward secrecy, no key rotation, no authentication of participants
- A determined attacker with access to memory or the shared secret can still read messages
- For learning/demo purposes only

### Choosing Which to Enable
| Feature | Protects Against | Requires Setup | Recommended For |
|---------|------------------|----------------|-----------------|
| TLS (wss) | Network sniffers / MITM on local network | Keystore | Any multi-host setup |
| Shared Secret | Server operator & other clients without secret | Shared secret out-of-band | Semi-private group chats |
| Both | Network + server snooping | Both above | Maximum available privacy here |

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
- **Crypto Utilities**: Basic encrypt/decrypt round trip and fallbacks

## Usage

1. **Connect**: The application auto-connects to localhost:8080
2. **(Optional)** Enter a shared Secret before connecting for encrypted messaging
3. **Set Username**: Enter your desired username before connecting
4. **Send Messages**: Type in the message field and press Enter or click Send
5. **View Messages**: All messages appear in the scrollable message area with timestamps

## Technical Details

- **Server**: Java-WebSocket library for WebSocket server implementation
- **Client**: WebSocket client with optional AES-GCM content encryption
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

## Security Notice
This project adds optional encryption for educational purposes. Do NOT rely on it for real confidential data. For production-grade secure messaging you'd need robust key exchange (e.g., X3DH/Double Ratchet), forward secrecy, authenticated participant management, certificate validation hardening, replay protection, and more.
