## Name

scheduler_set_parameters, scheduler_get_parameters - Set and get scheduler parameters for processes and threads

## Description

Modify or retrieve the scheduler parameters for processes or threads. `scheduler_set_parameters` will affect how the process or thread specified is scheduled the next time, so it might not have an immediately observable effect.

The parameter argument given to both system calls is defined as:

```**c++
struct SC_scheduler_parameters_params {
    pid_t pid_or_tid;
    SchedulerParametersMode mode;
    struct sched_param parameters;
};
```

-   `mode` is an enum taking the values `Process` and `Thread`. It specifies whether the syscalls handle whole-process scheduler parameters or thread-level scheduler parameters.
-   `pid_or_tid` specifies the process or thread to operate on, depending on `mode`.
-   `sched_param` is the parameters that are to be read or written for the process or thread specified with the other parameters. This struct is the POSIX-compliant data structure used in `sched_setparam` and others.

The only currently available scheduling parameter is the `int sched_priority`, the scheduling priority.

### Security

Both system calls require the `proc` promise.

There are the following limitations as to which process can modify which process' or thread's parameters:

-   The superuser can modify any process or thread scheduling parameters.
-   Any thread can modify the scheduling parameters of all of its process' threads and of the process itself.
-   Any process can modify the scheduling parameters of all processes that are owned by the same user (effective user ID and user ID must match). It cannot, however, modify the scheduling parameters of individual threads within the process.

## Return value

For `scheduler_get_parameters`, the retrieved parameters are written into the `sched_param` substructure. In either system call, a return value of 0 indicates success and a non-zero return value indicates an error.

## Errors

-   `EINVAL`: The scheduling parameters are invalid.
-   `EPERM`: The thread is not allowed to access the scheduling parameters for the given process or thread.
-   `ESRC`: The given process ID or thread ID does not refer to an existing process or thread.
-   `EFAULT`: The parameter structure pointer is invalid.

## History

The `scheduler_set_parameters` and `scheduler_get_parameters` syscalls replace the less generic `sched_setparam` and `sched_getparam` syscalls which precisely mirrored POSIX library functions.

<!-- FIXME: Add See also: Scheduler(7) once that manpage exists -->
