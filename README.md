# Mittere
Just a simple chat application build with ScalaFx and Akka.

## Introduction
We proposed a distributed chat system for our distributed system project.

## Features

- User can join a host by providing server IP address, port number and username.
- User can see who is online and which group chat is available.
- User can see who is offline.
- User can chat with other user.
- User can chat with a group of user through room.
- User can see who is typing in a chat.
- User can create a room by specifying a room name.
- Beautiful interface.

## Problem Encoutered

- Reliability
  - Original approach is, `Host` will keep track of all `Node` and `Room`. A seperate `RoomActor` will be created in the `Host` whenever the `Node` request to chat with other `Node`. The `RoomActor` is then responsible to keep track of the chat room information such as messages and users' `ActorRef`. It will then broadcast to every `Node` in the room when a message is received from the `Host`.
  - Basically, to chat, `Node` have to tell `Host` so `Host` will tell `RoomActor` to broadcast the message. This approach is not reliable and contains communication overhead. If the `Host` is disconnected, all the `Node` cannot communicate with each other. Furthermore, every time to send a message, the message is pass through several parties just to reach the target, which is inefficient.
  - We change the approach to, `Host` and `Node` both keep record of the information. The responsibility of the `Host` is to provide new `Node` the information needed.
  - No more `RoomActor` is involved in order for the `Node` to chat with other `Node`.
  - We just get the `ActorRef` from the information we keep track of and send it directly to the other `Node`. 
  - The new implementation does not rely on the `Host` anymore to pass the messages. So even if the `Host` fail, the core functionalty can still be carried on. The overhead is also further reduced as the `Node` directly communicate with each other.

- Fault Tolerance (Marcus)
  - In our first implementation, when the `Host` is disconnected, even though each `Node` can still communicate with each other, new `Node` is unable access the environment of the dead `Host` anymore.
  - Hence, we decided to allow the `Node` to setup as `Host` if their `Host` is disconnected. With this, new `Node` can still be connected to the same environment despite of the failure of the previous `Host`
  - When the `Host` is down, every `Node` will receive the notification. The problem is which `Node` should become the `Host`? We have to figure out a way to ensure that only one of the `Node` is converted into `Host`.
  - To solve this problem, we decided to use the `Node` which `name` is the smallest, which is consistent across every `Node`.
  - So every `Node` will send a message to inform the selected `Node` to become `Host`
  - If the `Node` has already received the message and become `Host`, it will ignore the same messages and inform all the other `Node` that it has become a `Host`.

- Messy code (Kai Wern)
  - The original implementation is implement all the functionality into one `Node` actor, which involves in handling join, creating chat room, chatting with other user and etc.
  - The original approach is not maintainable in long term. 
  - One of the possible approach is to use State, however since our architecture is peer to peer. This approach is less suitable. In the chat system, there is only two possible state, which is `Initial` and `Joined`. However just splitting into these 2 state still result in a huge chunk of code.
  - Hence we use another approach, inspired from [Akka tutorial](https://doc.akka.io/docs/akka/1.3.1/scala/tutorial-chat-server.html). In this approach, we utilize Scala `trait` to have a seperation of concern. Our `Node` actor is consists of three abstract methods `joinManangement`, `sessionManagement` and `chatManagement` which handle different category of messages seperately. Those methods are implemented in a seperate `trait`.

- Uniform Interface for Chat
  - During our original implementation, we focus just on getting one `Node` to chat with another `Node`. In our implementation, we use the term `User`, which consists of `username` and `ActorRef` to identify each `Node`.
  - Basically, we just keep track of a key value pairs of `username` and `ActorRef`, in order for us to look up the address so we can send the message to the remote `Node`. Each messages with other users is also tracked seperately in a key value pairs of `username` and `messages`, which is an `ArrayBuffer[Message]`.
  - Later on, we decided to add in `Room` which allow user to have a group chat with other users. We track a seperate key value pairs of `roomName` and `Room` object. The `Room` class contains of `name`, `Set[ActorRef]` and `ArrayBuffer[Message]`.
  - We have both `User` and `Room` class, with the trait `Chattable` at this stage. This makes our UI and `akka` implementation more complicated as the implementation is slightly different for both scenario.
  - If a user send a message to a `User`, we look up the user's `ActorRef` and send to it.
  - If a user send a message to a `Room`, we look up the room object and loop through the `users` and send the message to each of the user.
  - This implementation is redundant in some sense and increase the complexity of the code at both front end and back end part. 
  - Hence, we uniform the interface by just using `Room` alone. Since eventually, an one-to-one messaging is just a communication within a `Room` with just 2 user.
  - We implement the changes by creating `Room` for each online users in a `Node`. The `Room` are then identified by the name of both user, joined with ":". The ensure that the identifier is consistent, the first part of the identifier (before ":") will always be the smaller `String`.
  - With this approach, we are able to make the code more straighforward and simple, hence improve the maintainability of the chat system.

- Customizing GUI (Machi)


## Strength

- Heterogeneity
  - Works in different operating systems, because of JVM
- Fault Tolerance
  - Fault masking
- Reliability
  - Each node has data backup

## Weakness

- Data does not persist 
- Security

## Getting Started
```
$ sbt
[info] Loading project definition from /Users/kai/Desktop/ds-assignment-2/project
[info] Loading settings from build.sbt ...
[info] Set current project to chat (in build file:/Users/kai/Desktop/ds-assignment-2/)
[info] sbt server started at 127.0.0.1:4641
sbt:chat> run
```

If you face any issues, just ask in the group.

## Todo
All the tasks below are subject to change.

### FXML
- [X] Chat Screen (Similar to telegram/whatsapp without profile picture)
- [X] User & Chat Room List (Prefer to be sidebar with tab/accordian)
- [X] Make Chat Screen responsive

### Backend
- [X] Able to send and receive message to/from other user.
- [X] Able to see user is typing.
- [X] Able to see user is online.
- [X] Able to create a chat room for multiple user.
- [X] Able to join chat room, send and receive from/to the chat room.
- [X] Able to receive notification when someone receive message.

### Error Handling
- [X] Interface Binding should not crash while user do not enter integer.
- [X] Handle when incorrect port and localhost inserted.
- [X] Check if username is unique.
- [X] Check if group name is unique.
- [X] Remove user from online when disconnected.

