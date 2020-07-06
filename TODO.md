## To Do

- Create queueing mechanism for connection issues disrupting the websocket

## Done

- Fix bug where initially events are not sent off
- `sid` should be reset each time the app comes back to the foreground
- HEARTBEAT loop should stop when the app is in the background, and resume once the app comes back to the foreground (resetting the increment)
- Add Result and ResultSet classes
