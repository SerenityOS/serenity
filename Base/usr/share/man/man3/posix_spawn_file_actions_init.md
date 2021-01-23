## Name

`posix_spawn_file_actions` - configure file actions for `posix_spawn`

## Synopsis

```**c++
#include <spawn.h>

typedef struct posix_spawn_file_actions_t;

int posix_spawn_file_actions_init(posix_spawn_file_actions_t*);
int posix_spawn_file_actions_destroy(posix_spawn_file_actions_t*);

int posix_spawn_file_actions_addchdir(posix_spawn_file_actions_t*, const char*);
int posix_spawn_file_actions_addfchdir(posix_spawn_file_actions_t*, int);
int posix_spawn_file_actions_addclose(posix_spawn_file_actions_t*, int);
int posix_spawn_file_actions_adddup2(posix_spawn_file_actions_t*, int old_fd, int new_fd);
int posix_spawn_file_actions_addopen(posix_spawn_file_actions_t*, int fd, const char*, int flags, mode_t);
```

## Description

Configure a `posix_spawn_file_actions_t` object for use with [`posix_spawn`(2)](posix_spawn.md). This object can be used to let `posix_spawn()` set up file-related state for the spawned child process. The file actions are executed after creating the the new process but before loading its binary in the order they were added to the `posix_spawn_file_actions_t` object.

A `posix_spawn_file_actions_t` object is allocated on the stack but starts in an undefined state.

`posix_spawn_file_actions_init()` initializes a `posix_spawn_file_actions_t` object that is in an undefined state and puts it in a valid state. It has to be called before the object can be passed to any other function.

`posix_spawn_file_actions_destroy()` frees up resources used by a valid `posix_spawn_file_actions_t` object and puts it into an undefined state. It has to be called after a `posix_spawn_file_actions_t` object is no longer needed.

It is valid to alternatingly call `posix_spawn_file_actions_init()` and `posix_spawn_file_actions_destroy()` on the same object, 

`posix_spawn_file_actions_addchdir()` and `posix_spawn_file_actions_addfchdir()` make `posix_spawn()` change the current working directory before spawning a process, like `chdir` and `fchdir` would.. The current working directory affects the spawned child process, but also relative paths passed to later `posix_spawn_file_actions_add(f)chdir()` and `posix_spawn_file_actions_addopen()`, and relative paths passed to `posix_spawn()` for the executable path.

`posix_spawn_file_actions_addclose()` makes `posix_spawn()` close a file descriptor before spawning a process, like `close` would.

`posix_spawn_file_actions_addclose()` makes `posix_spawn()` dup a file descriptor before spawning a process, like `dup2` would.

`posix_spawn_file_actions_addopen()` makes `posix_spawn()` open a file with given flags and mode, like `open` would, and then makes it available under fd `fd` to the spawned process.

## Return value

In SerenityOS, these functions always succeed and return 0.

If the effect of a file action fails, the child will exit with exit code 127 before even executing the child binary.

## See also

* [`posix_spawn`(2)](posix_spawn.md)
