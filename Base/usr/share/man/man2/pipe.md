## Name

pipe, pipe2 - create a pipe

## Synopsis

```**c++
#include <unistd.h>

int pipe(int pipefd[2]);
int pipe2(int pipefd[2], int flags);
```

## Description

`pipe()` creates a new pipe, an anonymous FIFO channel. It returns two new file descriptors in `pipefd`.
Any data written to the `pipefd[1]` can then be read from `pipefd[0]`. When `pipefd[1]` is closed, reads
from `pipefd[0]` will return EOF.

`pipe2()` behaves the same as `pipe()`, but it additionally accepts the following _flags_:

-   `O_CLOEXEC`: Automatically close the file descriptors created by this call, as if by `close()` call, when performing an `exec()`.

## Examples

The following program creates a pipe, then forks, the child then
writes some data to the pipe which the parent reads:

```c++
#include <AK/Assertions.h>
#include <stdio.h>
#include <unistd.h>

int main()
{
    // Create the pipe.
    int pipefd[2];
    int rc = pipe(pipefd);
    VERIFY(rc == 0);

    pid_t pid = fork();
    VERIFY(pid >= 0);

    if (pid == 0) {
        // Close the reading end of the pipe.
        close(pipefd[0]);
        // Write a message to the writing end of the pipe.
        static const char greeting[] = "Hello friends!";
        int nwritten = write(pipefd[1], greeting, sizeof(greeting));
        VERIFY(nwritten == sizeof(greeting));
        exit(0);
    } else {
        // Close the writing end of the pipe.
        // If we don't do this, we'll never
        // get an EOF.
        close(pipefd[1]);
        // Read the message from the reading end of the pipe.
        char buffer[100];
        int nread = read(pipefd[0], buffer, sizeof(buffer));
        VERIFY(nread > 0);
        // Try to read again. We should get an EOF this time.
        nread = read(pipefd[0], buffer + nread, sizeof(buffer) - nread);
        VERIFY(nread == 0);
    }
}
```
