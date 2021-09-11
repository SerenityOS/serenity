## SerenityOS Sandboxing

SerenityOS provides pledge and unveil syscalls to achieve opt-in sandboxes for its applications. However for user software like image editors or PDF viewers we want users to have the choice of opening new files and adding them to the sandbox as needed.

### FileSystemAccessServer

Designed to be used after unveiling, the FileSystemAccessServer is an opt-in service which allows users to choose whether additional files on their system can be read from or written to.

The promise provided here is that only files and directories included in unveil calls or approved by a user action should be allowed to be read.

User actions that could indicate approval include:
* Pressing yes on a yes/no prompt
* Choosing a file or directory inside a file picker
* Dragging and dropping a file or directory onto a window (not currently supported)