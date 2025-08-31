# Advanced Chat Application

A modern, real-time chat application built with Java Socket Programming featuring a beautiful GUI interface where multiple clients can connect to a centralized server and chat with each other.

## 🎯 Features

### Server Features
- **Centralized Chat Server**: Multi-threaded server handling multiple concurrent connections
- **Real-time Monitoring**: Live server logs with timestamps and detailed activity tracking
- **User Management**: Automatic username assignment and tracking
- **Broadcast Messaging**: Send messages to all connected clients instantly
- **Connection Management**: Start/stop server with visual feedback and status indicators
- **Join/Leave Notifications**: Automatic notifications when users join or leave the chat
- **Thread-safe Operations**: Proper synchronization for concurrent client handling

### Client Features
- **Modern GUI Interface**: Beautiful, user-friendly chat interface with professional styling
- **Real-time Messaging**: Instant message delivery and display with timestamps
- **User Authentication**: Customizable usernames for each client
- **Online User List**: Live display of all connected users with count
- **Connection Status**: Visual indicators for connection state (green=connected, red=disconnected)
- **Message History**: Persistent chat history with auto-scrolling
- **Join/Leave Notifications**: See when users join or leave the chat room
- **Responsive Design**: Clean, modern UI that scales well

### Advanced Features
- **Structured Message Protocol**: Custom protocol with message types (CHAT, JOIN, LEAVE, SYSTEM)
- **Timestamp Formatting**: All messages include precise timestamps
- **Multi-threading**: Separate threads for message reading and UI updates
- **Error Handling**: Comprehensive error handling with user-friendly messages
- **Resource Management**: Automatic cleanup of connections and resources
- **Cross-platform**: Works on Windows, macOS, and Linux

## 📁 Project Structure

```
Chat Application/
├── Server.java              # Advanced GUI Chat Server
├── Client.java              # Original Console Client
├── AdvancedClient.java      # Modern GUI Chat Client
└── README.md               # This documentation
```

## � How to Run

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- Java Swing (included with JDK)

### Quick Start
```bash
# Compile all files
javac *.java

# Start the server (in one terminal)
java Server

# Start clients (in separate terminals)
java AdvancedClient
```

### Detailed Instructions

#### 1. Compile the Project
```bash
javac Server.java AdvancedClient.java
```

#### 2. Start the Chat Server
```bash
java Server
```
- Click "Start Server" to begin accepting connections on port 12345
- Monitor all server activity in the log area
- View connected clients in the client list
- Server shows real-time timestamps for all events

#### 3. Connect Chat Clients
```bash
java AdvancedClient
```
- Enter your desired username
- Set server address (default: localhost) and port (default: 12345)
- Click "Connect" to join the chat room
- Start chatting with other connected users!

## 💬 Chat Features

### Message Types
- **Regular Messages**: `[HH:mm:ss] Username: Hello everyone!`
- **System Messages**: `[HH:mm:ss] * Welcome to the chat, John!`
- **Join Notifications**: `[HH:mm:ss] *** Alice joined the chat ***`
- **Leave Notifications**: `[HH:mm:ss] *** Bob left the chat ***`

### Commands
- `/list` - Show all connected users
- `/quit` - Leave the chat room

### User Interface
- **Connection Panel**: Server settings, username, and connection controls
- **Chat Area**: Main message display with timestamps and formatting
- **User List**: Shows all online users with live count updates
- **Message Input**: Send messages with Enter key or Send button
- **Status Indicators**: Visual connection status (green/red)

## 🔧 Technical Details

### Server Architecture
- **ServerSocket**: Accepts incoming client connections on port 12345
- **ExecutorService**: Manages thread pool for client handlers
- **ConcurrentHashMap**: Thread-safe storage of connected clients and usernames
- **Message Broadcasting**: Efficient message distribution to all clients
- **Swing EDT**: Proper GUI updates on Event Dispatch Thread

### Client Architecture
- **Socket Connection**: TCP connection to chat server
- **Message Reader Thread**: Dedicated thread for receiving messages
- **Protocol Parser**: Parses structured messages (CHAT|timestamp|sender|message)
- **Swing Components**: Modern GUI with custom styling and layout
- **Auto-scroll**: Chat area automatically scrolls to latest messages

### Message Protocol
```
CHAT|14:30:25|Alice|Hello everyone!
JOIN|14:30:15|Bob joined the chat
LEAVE|14:35:42|Alice left the chat
SYSTEM|14:30:10|Welcome to the server!
USERLIST|14:30:30|Alice,Bob,Charlie
```

## 🎨 UI Features

### Server Interface
- Clean, professional server management interface
- Real-time activity log with timestamps
- Connected clients list with live updates
- Start/Stop server controls with status feedback
- Comprehensive error logging and monitoring

### Client Interface
- Modern chat application appearance
- Segoe UI font for better readability
- Color-coded status indicators
- Bordered sections for organized layout
- Responsive design that works on different screen sizes
- Auto-updating user count in title bars

## 🌟 Key Improvements Over Basic Socket Programming

1. **From Echo Server to Group Chat**: Messages are broadcast to all users instead of just echoing back
2. **User Management**: Proper username handling and user tracking
3. **Real-time Updates**: Live user list and connection status updates
4. **Professional UI**: Modern, polished interface instead of console
5. **Message Formatting**: Structured messages with timestamps and types
6. **Join/Leave Notifications**: Users see when others join or leave
7. **Thread Safety**: Proper synchronization for concurrent operations
8. **Error Recovery**: Graceful handling of connection losses and errors

## 🚀 Future Enhancements

- **Private Messaging**: Direct messages between specific users
- **File Sharing**: Send images, documents, and files
- **Message History**: Persistent chat history across sessions
- **User Authentication**: Login system with passwords
- **Chat Rooms**: Multiple chat rooms/channels
- **Emojis**: Emoji support in messages
- **Message Encryption**: Secure communication with SSL/TLS
- **Mobile Client**: Companion mobile app
- **Voice Chat**: Audio messaging capabilities

## 🐛 Troubleshooting

### Common Issues
1. **Port Already in Use**: Change port number in code or close other applications
2. **Connection Refused**: Ensure server is running and port is accessible
3. **GUI Not Appearing**: Check Java version and Swing support
4. **Messages Not Appearing**: Check firewall settings and network connectivity

### Debug Information
- Server logs show all connection attempts and message activity
- Client status indicators show connection state
- Use `/list` command to verify connected users
- Check timestamps for message timing verification

## 📝 Learning Outcomes

This project demonstrates:
- Advanced socket programming with custom protocols
- Multi-threaded server architecture
- GUI development with Java Swing
- Real-time network communication
- User interface design principles
- Message parsing and protocol design
- Thread synchronization and safety
- Error handling in distributed systems
- Client-server application patterns

---

**🎉 Happy Chatting!**

Built with ❤️ using Java Socket Programming - Now featuring a complete chat application experience!