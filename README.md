# Mittere
Just a simple chat application build with ScalaFx and Akka.

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
- [ ] Make Chat Screen responsive
- [ ] Dialog for About

### Backend
- [X] Able to send and receive message to/from other user.
- [X] Able to see user is typing.
- [X] Able to see user is online.
- [X] Able to create a chat room for multiple user.
- [X] Able to join chat room, send and receive from/to the chat room.
- [ ] Able to receive notification when someone receive message.
- [ ] Able to kick user out of chat room. _(Optional)_
- [ ] Able to delete chat room. _(Optional)_

### Error Handling
- [X] Interface Binding should not crash while user do not enter integer.
- [ ] **Handle when incorrect port and localhost inserted.** 
- [ ] **Check if username is unique.**
- [ ] **Check if group name is unique.**
- [X] Remove user from online when disconnected.








