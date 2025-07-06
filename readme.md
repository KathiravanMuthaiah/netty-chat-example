# 📡 Netty Chat Example

This project demonstrates **Netty-based TCP communication** in Java 21, where each node behaves both as a **server** and a **client**, forwarding enriched messages to peer nodes.

## ✨ Features

- Non-blocking TCP server and client in a single process
- Simple message enrichment (adds random suffix)
- Multi-node chat simulation
- Java 21 compatibility
- Minimal dependencies (`netty-all`)

## 📂 Project Structure

```structured text
netty-chat-example/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── ChatNode.java
```

## 🚀 How to Build

```bash
mvn clean package
```

## 🏃 How to Run

Start 3 nodes in separate terminals:

**Node 1:**

```bash
mvn clean compile --global-toolchains toolchains.xml exec:java -Dexec.mainClass="com.example.ChatNode" -Dexec.args="9001 localhost:9002,localhost:9003"
```

**Node 2:**

```bash
mvn clean compile --global-toolchains toolchains.xml exec:java -Dexec.mainClass="com.example.ChatNode" -Dexec.args="9002 localhost:9001,localhost:9003"
```

**Node 3:**

```bash
mvn clean compile --global-toolchains toolchains.xml exec:java -Dexec.mainClass="com.example.ChatNode" -Dexec.args="9003 localhost:9001,localhost:9002"
```

To send a message:

```bash
echo "Hello from outside" | nc localhost 9001
```

The message will:

✅ Be printed by Node1
 ✅ Enriched with random data
 ✅ Forwarded to Node2 and Node3

## 🛑 Known Limitations

- No message deduplication—messages may loop if forwarded back.
- No TLS encryption.
- No persistence or clustering logic.

## 📘 License

MIT License.