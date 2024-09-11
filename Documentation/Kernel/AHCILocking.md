# AHCI Locking

## Introduction to hard locks, soft locks and what they do

### Soft lock - `Lock`

A soft lock is basically a regular lock in the kernel. We use it
with a `Locker` class, to create a scoped locking of that lock:

```c++
Locker locker(m_lock);

...
...

return true;
```

This lock doesn't disable interrupts at all, and if it is already in use, the scheduler will simply yield away from that section until it tries to lock it again.

### Hard lock - `Spinlock`

A hard lock is essentially a lock that is used in critical sections in the kernel. We use it with a `ScopedSpinLock` class, to create a scoped locking of that lock:

```c++
ScopedSpinLock lock(m_lock);

...
...

return true;
```

### Why do we need soft and hard locking in the AHCI code?

First of all, the proper way of taking a `SpinLock` and `Lock` is to:

```c++
Locker locker(m_soft_lock);
ScopedSpinLock lock(m_spinlock);

...
...

return true;
```

This sequence is relevant for any pattern of taking a soft and hard lock together in the kernel.
The reason for this order is that `SpinLock` will disable interrupts, while `Lock` will still allow the system to yield execution
to another thread if we can't lock the soft lock, because interrupts are not disabled. Taking a `SpinLock` and then a `Lock` is considered a bug, because we already disabled interrupts so yielding from this section is not possible anymore.

We need both types of locking to implement hardware access safely.
When we use the `SpinLock` object, we ensure that only one CPU can run the scoped code section without any interruptions at all. This is important, because interrupts can be fatal in essentially what is a critical section.

We use the `Lock` object for basically anything else, most of the time together with `SpinLock` as described earlier. This object becomes important when we schedule IO work to happen in the IO `WorkQueue`.
When we run in `WorkQueue`, it is guaranteed that we will have interrupts enabled - therefore we will not use the `SpinLock` to allow the kernel to handle page fault interrupts, but we still want to ensure no other concurrent operation can happen, so we still hold the `Lock`.
