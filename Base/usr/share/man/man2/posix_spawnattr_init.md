## Name

posix\_spawnattr - configure attributes for posix\_spawn

## Synopsis

```**c++
#include <spawn.h>

POSIX_SPAWN_RESETIDS
POSIX_SPAWN_SETPGROUP
POSIX_SPAWN_SETSCHEDPARAM
POSIX_SPAWN_SETSCHEDULER
POSIX_SPAWN_SETSIGDEF
POSIX_SPAWN_SETSIGMASK
POSIX_SPAWN_SETSID

struct posix_spawnattr_t;

int posix_spawnattr_init(posix_spawnattr_t*);
int posix_spawnattr_destroy(posix_spawnattr_t*);

int posix_spawnattr_getflags(const posix_spawnattr_t*, short*);
int posix_spawnattr_getpgroup(const posix_spawnattr_t*, pid_t*);
int posix_spawnattr_getschedparam(const posix_spawnattr_t*, struct sched_param*);
int posix_spawnattr_getschedpolicy(const posix_spawnattr_t*, int*);
int posix_spawnattr_getsigdefault(const posix_spawnattr_t*, sigset_t*);
int posix_spawnattr_getsigmask(const posix_spawnattr_t*, sigset_t*);
int posix_spawnattr_setflags(posix_spawnattr_t*, short);
int posix_spawnattr_setpgroup(posix_spawnattr_t*, pid_t);
int posix_spawnattr_setschedparam(posix_spawnattr_t*, const struct sched_param*);
int posix_spawnattr_setschedpolicy(posix_spawnattr_t*, int);
int posix_spawnattr_setsigdefault(posix_spawnattr_t*, const sigset_t*);
int posix_spawnattr_setsigmask(posix_spawnattr_t*, const sigset_t*);
```

## Description

Configures a `posix_spawnattr_t` object for use with [`posix_spawn`(2)](posix_spawn.md). This object can be used to let `posix_spawn()` set up process attributes for the spawned child process. The file actions are executed after creating the new process but before loading its binary.

A `posix_spawnattr_t` object is allocated on the stack but starts in an undefined state.

`posix_spawnattr_init()` initializes a `posix_spawnattr_t` object that is in an undefined state and puts it in a valid state. It has to be called before the object can be passed to any other function.

`posix_spawnattr_destroy()` frees up resources used by a valid `posix_spawn_file_actions_t` object and puts it into an undefined state. It has to be called after a `posix_spawnattr_t` object is no longer needed.

It is valid to alternatingly call `posix_spawnattr_init()` and `posix_spawnattr_destroy()` on the same object, 

`posix_spawnattr_setflags()` configures which attributes of the new child process `posix_spawn()` will set. It receives a bitmask that can contain:

* `POSIX_SPAWN_RESETIDS`: If set, `posix_spawn()` will reset the effective uid and gid of the child process to the real uid and gid of the parent process. See also [`setuid_overview`(7)](../man7/setuid_overview.md).

* `POSIX_SPAWN_SETPGROUP`: If set, `posix_spawn()` will set the process group ID of the child process to the process group ID configured with `posix_spawnattr_setpgroup()`, as if `setpgid(0, pgroup)` was called in the child process. The behavior if both this and `POSIX_SPAWN_SETSID` is set is undefined.

* `POSIX_SPAWN_SETSCHEDPARAM`: If set, `posix_spawn()` will set the scheduler parameter of the child process to the process group ID configured with `posix_spawnattr_setschedparam()`, as if `sched_setparam(0, schedparam)` was called in the child process.

* `POSIX_SPAWN_SETSCHEDULER`: This is not yet implemented in SerenityOS.

* `POSIX_SPAWN_SETSIGDEF`: If set, `posix_spawn()` will reset the signal handlers of the child process configured with `posix_spawnattr_setsigdefault()` to each signal's default handler.

* `POSIX_SPAWN_SETSIGMASK`: If set, `posix_spawn()` will set the signal mask of the child process to the signal mask configured with `posix_spawnattr_setsigmask()`, as if `sigprocmask()` was called in the child process.

* `POSIX_SPAWN_SETSID`: If set, `posix_spawn()` will run the child process in a new session, as if `setsid()` was called in the child process. The behavior if bboth this an d`POSIX_SPAWN_SETPGROUP` is set is undefined.

The `posix_spawnattr_get*` functions return what's been set with the corresponding setters. The default `flags` and `pgroup` are 0, the default `sigdefault` set is `sigemptyset()`, all other fields have an unspecified default value.


## Return value

In SerenityOS, these functions always succeed and return 0.

The one exception is `posix_spawnattr_setflags()`, which can return -1 and set `errno` to `EINVAL` if an unknown bit is set in the passed bitmask.

If the effect of an attr fails, the child will exit with exit code 127 before even executing the child binary.

## See also

* [`posix_spawn`(2)](posix_spawn.md)
