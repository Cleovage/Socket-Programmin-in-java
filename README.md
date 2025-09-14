# Advanced Socket Programming Chat Application

A modern Java chat application featuring a professional GUI for both server and client, built with sockets and custom UI components.

## Table of Contents
- [Features](#features)
- [Screenshots](#screenshots)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Learning Outcomes](#learning-outcomes)
- [License](#license)

## Features
- Multi-tab server dashboard: live stats, logs, client management, broadcast, settings
- Real-time chat client: modern UI, emoji picker, user list, notifications
- Custom UI components: ModernButton, ModernTextField, ModernPanel, ModernScrollBarUI
- Dark theme, responsive design, cross-platform font support
- Thread-safe, multi-threaded server with structured message protocol
- Emoji support and notifications

## Screenshots
> _Add screenshots here to showcase the UI_

## Getting Started
### Prerequisites
- Java Development Kit (JDK) 8 or higher
- Windows, Linux, or macOS

### Build & Run
1. Compile all Java files:
   ```sh
   javac *.java
   ```
2. Start the server:
   ```sh
   java Server
   ```
3. Start the client:
   ```sh
   java AdvancedClient
   ```

## Project Structure
```
Socket-Programmin-in-java/
├── Server.java           # GUI server
├── AdvancedClient.java   # GUI client
├── Client.java           # Console client
├── ModernUI.java         # Custom UI components
├── ...                   # Other files
```

## Troubleshooting
- **Server won't start:** Check port 12345 availability, verify Java 8+
- **Client can't connect:** Ensure server is running, check firewall
- **GUI issues:** Confirm Java Swing and font availability

## Learning Outcomes
- Socket programming (TCP client-server)
- Multi-threading and synchronization
- Swing GUI development and custom components
- Error handling and resource management

## License
MIT

---
**Built with ❤️ using Java Socket Programming**