## Name

futex - low-level synchronization primitive

## Synopsis

```c++
#include <serenity.h>

// Raw syscall.
int futex(uint32_t* userspace_address, int futex_op, uint32_t value, const struct timespec* timeout, uint32_t* userspace_address2, uint32_t value3);

// More convenient wrappers.
int futex_wait(uint32_t* userspace_address, uint32_t value, const struct timespec* abstime, int clockid, int process_shared);
int futex_wake(uint32_t* userspace_address, uint32_t count, int process_shared);
```

## Description

The `futex()` system call provides a low-level synchronization primitive,
essentially exposing the kernel's internal thread synchronization primitives
to userspace.

While the `futex()` API is powerful and generic, it is complex and cumbersome
to use, and notoriously tricky to use _correctly_. For this reason, it is not
intended to be used by application code directly, but rather to serve as
a building block for more specialized and easier to use synchronization
primitives implemented in user space, such as mutexes and semaphores.
Specifically, the `futex()` API is designed to enable userspace synchronization
primitives to have a _fast path_ that does not involve calling into the kernel
at all in the common uncontended case, avoiding the cost of making a syscall
completely.

_A futex_ is a single 32-bit integer cell located anywhere in the address space
of a process (identified by its address), as well as an associated kernel-side
queue of waiting threads. The kernel-side resources associated with a futex are
created and destroyed implicitly when a futex is used; in other words, any
32-bit integer can be used as a futex without any specific setup, and a futex
on which no threads are waiting is no different to any other integer. The
kernel does not assign any meaning to the value of the futex integer; it is up
to userspace to make use of the value for its own logic.

The `futex()` API provides a number of _operations_, the most basic ones being
_waiting_ and _waking_:

-   `FUTEX_WAKE` / `futex_wake()`: wake up to `count` threads waiting on the
    futex (in the raw `futex()` syscall, `count` is passed as the `value`
    argument). The two most common values for `count` are 1 (wake a single
    thread) and `UINT32_MAX` (wake all threads).
-   `FUTEX_WAIT` / `futex_wait()`: wait on the futex, but only if the current
    value of the futex integer matches the specified `value`. The value
    comparison and blocking is done atomically: if another thread changes the
    value before the calling thread starts waiting, the calling thread will not
    begin waiting at all, and the `futex_wait()` call will return `EAGAIN`
    immediately. A waiting thread may wake up spuriously, without a matching call
    to `futex_wake()`.
-   `FUTEX_WAKE_BITSET`: like `FUTEX_WAKE`, but only consider waiting threads
    that have specified a matching bitset (passed in `value3`). Two bitsets match
    if their _bitwise and_ is non-zero. A thread that has not specified a bitset
    is treated as having a bitset with all bits set (`FUTEX_BITSET_MATCH_ANY`,
    equal to `0xffffffff`).
-   `FUTEX_WAIT_BITSET`: like `FUTEX_WAIT`, but the thread will only get woken by
    wake operations specifying a matching bitset.
-   `FUTEX_REQUEUE`: wake up to `value` threads waiting on the futex, and requeue
    up to `value2` (passed instead of the `timeout` argument) of the remaining
    waiting threads to wait on another futex specified by `userspace_address2`,
    without waking them up. Waking and requeueing threads is done atomically.

    Requeueing threads without waking them up is useful to avoid "thundering
    herd" issues with synchronization primitives like condition variables, where
    multiple threads may wait for an event, but an event can only be handled by a
    single thread at a time.

-   `FUTEX_CMP_REQUEUE`: like `FUTEX_REQUEUE`, but only if the current value of
    the futex integer matches the specified `value3`. The value comparison,
    waking and requeueing threads are all done atomically.
-   `FUTEX_WAKE_OP`: modify the value of the futex specified by
    `userspace_address2`, wake up to `value` threads waiting on the futex, and
    optionally up to `value2` (passed instead of the `timeout` argument) threads
    waiting on the futex specified by `userspace_address2`.

    The details of this operation are not currently documented here, see the
    implementation for details.

Additionally, the `FUTEX_PRIVATE_FLAG` flag can be _or_'ed in with one of the
_operation_ values listed above. This flag restricts the call to only work on
other threads of the same process (as opposed to any threads in the system that
may have the same memory page mapped into their address space, possibly at a
different address), which enables additional optimizations in the syscall
implementation. The inverse of this flag is exposed as the `process_shared`
argument in `futex_wait()` and `futex_wake()` wrapper functions.

## Return value

-   `FUTEX_WAKE`, `FUTEX_WAKE_BITSET`, `FUTEX_WAKE_OP`: the number of the waiting
    threads that have been woken up, which may be 0 or a positive number.
-   `FUTEX_WAIT`, `FUTEX_WAIT_BITSET`: 0 if blocked and got woken up by an
    explicit wake call or woke up spuriously, an error otherwise.
-   `FUTEX_REQUEUE`, `FUTEX_CMP_REQUEUE`: the total number of threads woken up
    and requeued.

## Errors

-   `EAGAIN`: for wait operations, did not begin waiting, because the futex value
    has already been changed.
-   `ETIMEDOUT`: for wait operations with a timeout, timed out.
-   `EFAULT`: the specified futex address is invalid.
-   `ENOSYS`: `FUTEX_CLOCK_REALTIME` was specified, but the operation is not
    `FUTEX_WAIT` or `FUTEX_WAIT_BITSET`.
-   `EINVAL`: The arithmetic-logical operation for `FUTEX_WAKE_OP` is invalid.

## Examples

The following program demonstrates how futexes can be used to implement a
simple "event" synchronization primitive. An event has a boolean state: it can
be _set_ or _unset_; the initial state being unset. The two operations on an
event are _waiting_ until it is set, and _setting_ it (which wakes up any
threads that were waiting for the event to get set).

Such a synchronization primitive could be used, for example, to notify threads
that are waiting for another thread to perform some sort of complex
initialization.

The implementation features two fast paths: both setting an event that no
thread is waiting on, and trying to wait on an event that has already been set,
are performed entirely in userspace without calling into the kernel. For this
to work, the value of the futex integer is used to track both the state of the
event (whether it has been set) and whether any threads are waiting on it.

```c++
#include <AK/Atomic.h>
#include <serenity.h>

class Event {
private:
    enum State : u32 {
        UnsetNoWaiters,
        UnsetWithWaiters,
        Set,
    };

    AK::Atomic<State> m_state { UnsetNoWaiters };

    u32* state_futex_ptr() { return reinterpret_cast<u32*>(const_cast<State*>(m_state.ptr())); }

public:
    void set()
    {
        State previous_state = m_state.exchange(Set, AK::memory_order_release);
        // If there was anyone waiting, wake them all up.
        // Fast path: no one was waiting, so we're done.
        if (previous_state == UnsetWithWaiters)
            futex_wake(state_futex_ptr(), UINT32_MAX, false);
    }

    void wait()
    {
        // If the state is UnsetNoWaiters, set it to UnsetWithWaiters.
        State expected_state = UnsetNoWaiters;
        bool have_exchanged = m_state.compare_exchange_strong(
            expected_state, UnsetWithWaiters,
            AK::memory_order_acquire);
        if (have_exchanged)
            expected_state = UnsetWithWaiters;

        // We need to check the state in a loop and not just once
        // because of the possibility of spurious wakeups.
        // Fast path: if the state was already Set, we're done.
        while (expected_state != Set) {
            futex_wait(state_futex_ptr(), expected_state, nullptr, 0, false);
            expected_state = m_state.load(AK::memory_order_acquire);
        }
    }
};
```

## History

The name "futex" stands for "fast userspace mutex".

The `futex()` system call originally appeared in Linux. Since then, many other
kernels implemented support for futex-like operations, under various names, in
particular:

-   Darwin (XNU) has private `ulock_wait()` and `ulock_wake()` API;
-   Windows (NT) apparently has `WaitOnAddress()`, `WakeByAddressSingle()` and
    `WakeByAddressAll()`;
-   FreeBSD and DargonFly BSD have `umtx`;
-   OpenBSD has Linux-like `futex()`;
-   GNU Hurd has `gsync_wait()`, `gsync_wake()`, and `gsync_requeue()`.

## Further reading

-   [Futexes Are Tricky](https://akkadia.org/drepper/futex.pdf) by Ulrich Drepper
-   [Locking in WebKit](https://webkit.org/blog/6161/locking-in-webkit/) by Filip Pizlo
