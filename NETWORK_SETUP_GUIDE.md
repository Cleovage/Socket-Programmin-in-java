# 🌐 Local Network Setup Guide

## Quick Start: Connect Multiple PCs on WiFi

### Step 1: Start the Server 🖥️
1. **Choose one laptop** to run the server
2. Make sure it's connected to your WiFi network
3. Run: `java Server`
4. The server will automatically detect and display your network IP (e.g., `192.168.1.5:12345`)
5. **Click on the network IP** to copy it to clipboard
6. **Share this IP address** with others who want to connect

### Step 2: Connect Clients 💻
1. On **other laptops** on the same WiFi network
2. Run: `java AdvancedClient`
3. In the **Server** field, enter the server's IP address (e.g., `192.168.1.5`)
4. Enter the **Port** (default: `12345`)
5. Enter your **Username**
6. Click **Connect** 🔗

### Step 3: Start Chatting! 💬
All clients will now be connected to the server and can chat with each other!

---

## Troubleshooting 🔧

### Can't Connect?
- ✅ Make sure all devices are on the **same WiFi network**
- ✅ Check that the server is **running** before clients try to connect
- ✅ Verify the **IP address** is correct (check the server's toolbar)
- ✅ Check **Windows Firewall** settings:
  - Go to: Control Panel → Windows Defender Firewall → Allow an app
  - Add Java or allow port 12345

### Find Your Server IP Manually
**Windows:**
```cmd
ipconfig
```
Look for "IPv4 Address" under your WiFi adapter (usually starts with 192.168.x.x)

**Mac/Linux:**
```bash
ifconfig
```
Look for your WiFi interface (inet address)

---

## Network Requirements 📡

- All devices must be on the **same local network** (WiFi/Ethernet)
- Default port: **12345** (customizable in server UI)
- Protocol: **TCP/IP sockets**
- No internet required - works on **local network only**

---

## Advanced: Custom Port

1. In the server, change the port number before clicking Start
2. Make sure clients use the **same port number** when connecting
3. If using firewall, allow both the **old and new port**

---

## Features Working Over Network ✨

✅ Real-time messaging  
✅ User join/leave notifications  
✅ Private messages (@username)  
✅ Typing indicators  
✅ User list synchronization  
✅ Emoji support  
✅ File transfer notifications  
✅ Heartbeat/connection monitoring  

---

## Example Setup

**Laptop 1 (Server):**
- IP: 192.168.1.10
- Port: 12345
- Status: Running ✅

**Laptop 2 (Client - Alice):**
- Server: 192.168.1.10
- Port: 12345
- Username: Alice
- Status: Connected ✅

**Laptop 3 (Client - Bob):**
- Server: 192.168.1.10
- Port: 12345
- Username: Bob
- Status: Connected ✅

Now Alice and Bob can chat through the server! 🎉

---

## Security Note 🔒

This chat system is designed for **trusted local networks** (home, office).  
- No encryption (messages are plain text)
- No authentication required
- Anyone on the network can connect if they know the IP:Port

For secure communication over the internet, additional security measures would be needed.
