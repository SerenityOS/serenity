## Name

posix\_spawn - launch a new process

## Synopsis

```**c++
#include <spawn.h>

int posix_spawn(pid_t* pid, const char* executable_path, const posix_spawn_file_actions_t*, const posix_spawnattr_t*, char* const argv[], char* const envp[]);
int posix_spawnp(pid_t* pid, const char* executable_path, const posix_spawn_file_actions_t*, const posix_spawnattr_t*, char* const argv[], char* const envp[]);
```

## Description

Spawn a new process reading the binary `executable_path`, passing `argv` as arguments to `main()` and setting `envp` as argument.

Places the process ID of the new process in `pid`.

If `executable_path` passed to `posix_spawn` is a relative path, it is resolved relative to the current working directory.

If `executable_path` passed to `posix_spawnp` is a relative path, it is resolved by searching through directories specified in the `PATH` environment variable.

The `posix_spawn_file_actions_t` and `posix_spawnattr_t` arguments may be `nullptr`. If they aren't, see [`posix_spawn_file_actions`(2)](posix_spawn_file_actions_init.md) and [`posix_spawnattr`(2)](posix_spawnattr_init.md) for what they do.

The last entry in `argv` and `envp` has to be `nullptr`.

The new process is started as if the following steps are executed in this order:

1. A new process is started as if `fork()` was called.
2. If the `posix_spawnattr_t` parameter is non-nullptr, it [takes effect](posix_spawnattr_init.md).
3. If the `posix_spawn_file_actions_t` parameter is non-nullptr, it [takes effect](posix_spawn_file_actions_init.md).
4. `executable_path` is loaded and starts running, as if `execve` or `execvpe` was called.

## Return value

If the process is successfully forked, returns 0.
Otherwise, returns an error number. This function does *not* return -1 on error and does *not* set `errno` like most other functions, it instead returns what other functions set `errno` to as result.

If the process forks successfully but spawnattr or file action processing or exec fail, `posix_spawn` returns 0 and the child exits with exit code `127`.

## Example

This simple example launches `/bin/Calculator`.

To make the child process use the parent's environment, it passes `environ` from `unistd.h`.

```**c++
#include <errno.h>
#include <unistd.h>
#include <stdio.h>
#include <spawn.h>

int main()
{
    const char* argv[] = { "Calculator", nullptr };
    pid_t child_pid;
    if ((errno = posix_spawn(&child_pid, "/bin/Calculator", nullptr, nullptr, const_cast<char**>(argv), environ)))
        perror("posix_spawn");
}
```

## See also

* [`posix_spawnattr`(2)](posix_spawnattr_init.md)
* [`posix_spawn_file_actions`(2)](posix_spawn_file_actions_init.md)
