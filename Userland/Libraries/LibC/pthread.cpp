/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/SinglyLinkedList.h>
#include <Kernel/API/Syscall.h>
#include <LibSystem/syscall.h>
#include <bits/pthread_cancel.h>
#include <bits/pthread_integration.h>
#include <errno.h>
#include <limits.h>
#include <mallocdefs.h>
#include <pthread.h>
#include <serenity.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <syscall.h>
#include <time.h>
#include <unistd.h>

namespace {
using PthreadAttrImpl = Syscall::SC_create_thread_params;

} // end anonymous namespace

static constexpr size_t required_stack_alignment = 4 * MiB;
static constexpr size_t highest_reasonable_guard_size = 32 * PAGE_SIZE;

__thread void* s_stack_location;
__thread size_t s_stack_size;

__thread int s_thread_cancel_state = PTHREAD_CANCEL_ENABLE;
__thread int s_thread_cancel_type = PTHREAD_CANCEL_DEFERRED;

#define __RETURN_PTHREAD_ERROR(rc) \
    return ((rc) < 0 ? -(rc) : 0)

struct CleanupHandler {
    void (*routine)(void*);
    void* argument;
};

static thread_local SinglyLinkedList<CleanupHandler> cleanup_handlers;

static __thread bool pending_cancellation = false;

[[gnu::weak]] extern ErrorOr<FlatPtr> __create_new_tls_region() asm("__create_new_tls_region");
[[gnu::weak]] extern ErrorOr<void> __free_tls_region(FlatPtr thread_pointer) asm("__free_tls_region");

extern "C" {

[[noreturn]] static void exit_thread(void* code, void* stack_location, size_t stack_size)
{
    __pthread_key_destroy_for_current_thread();
    MUST(__free_tls_region(bit_cast<FlatPtr>(__builtin_thread_pointer())));
    syscall(SC_exit_thread, code, stack_location, stack_size);
    VERIFY_NOT_REACHED();
}

[[noreturn]] static void pthread_exit_without_cleanup_handlers(void* value_ptr)
{
    exit_thread(value_ptr, s_stack_location, s_stack_size);
}

static void* pthread_create_helper(void* (*routine)(void*), void* argument, void* stack_location, size_t stack_size)
{
    s_stack_location = stack_location;
    s_stack_size = stack_size;
    void* ret_val = routine(argument);
    pthread_exit_without_cleanup_handlers(ret_val);
}

static int create_thread(pthread_t* thread, void* (*entry)(void*), void* argument, PthreadAttrImpl* thread_params)
{
    void** stack = (void**)((uintptr_t)thread_params->stack_location + thread_params->stack_size);

    auto push_on_stack = [&](void* data) {
        stack--;
        *stack = data;
        thread_params->stack_size -= sizeof(void*);
    };

    // We set up the stack for pthread_create_helper.
    // Note that we need to align the stack to 16B, accounting for
    // the fact that we also push 16 bytes.
    while (((uintptr_t)stack - 16) % 16 != 0)
        push_on_stack(nullptr);

    thread_params->entry = entry;
    thread_params->entry_argument = argument;

    auto maybe_thread_pointer = __create_new_tls_region();
    if (maybe_thread_pointer.is_error())
        return maybe_thread_pointer.error().code();

    thread_params->tls_pointer = bit_cast<void*>(maybe_thread_pointer.release_value());

    VERIFY((uintptr_t)stack % 16 == 0);

#if ARCH(X86_64)
    // Push a fake return address
    push_on_stack(nullptr);
#endif

    int rc = syscall(SC_create_thread, pthread_create_helper, thread_params);
    if (rc >= 0)
        *thread = rc;
    else
        MUST(__free_tls_region(bit_cast<FlatPtr>(thread_params->tls_pointer)));

    __RETURN_PTHREAD_ERROR(rc);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_create.html
int pthread_create(pthread_t* thread, pthread_attr_t const* attributes, void* (*start_routine)(void*), void* argument_to_start_routine)
{
    if (!thread)
        return -EINVAL;

    PthreadAttrImpl default_attributes {};
    PthreadAttrImpl* const* arg_attributes = reinterpret_cast<PthreadAttrImpl* const*>(attributes);

    PthreadAttrImpl* used_attributes = arg_attributes ? *arg_attributes : &default_attributes;

    if (!used_attributes->stack_location) {
        // adjust stack size, user might have called setstacksize, which has no restrictions on size/alignment
        if (0 != (used_attributes->stack_size % required_stack_alignment))
            used_attributes->stack_size += required_stack_alignment - (used_attributes->stack_size % required_stack_alignment);

        used_attributes->stack_location = mmap_with_name(nullptr, used_attributes->stack_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS | MAP_STACK, 0, 0, "Thread stack");
        if (!used_attributes->stack_location)
            return -1;
    }

    dbgln_if(PTHREAD_DEBUG, "pthread_create: Creating thread with attributes at {}, detach state {}, priority {}, guard page size {}, stack size {}, stack location {}",
        used_attributes,
        (PTHREAD_CREATE_JOINABLE == used_attributes->detach_state) ? "joinable" : "detached",
        used_attributes->schedule_priority,
        used_attributes->guard_page_size,
        used_attributes->stack_size,
        used_attributes->stack_location);

    return create_thread(thread, start_routine, argument_to_start_routine, used_attributes);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_exit.html
void pthread_exit(void* value_ptr)
{
    while (!cleanup_handlers.is_empty()) {
        auto handler = cleanup_handlers.take_first();
        handler.routine(handler.argument);
    }

    pthread_exit_without_cleanup_handlers(value_ptr);
}

#ifndef _DYNAMIC_LOADER
void __pthread_maybe_cancel()
{
    // Check if we have cancellations enabled.
    if (s_thread_cancel_state != PTHREAD_CANCEL_ENABLE)
        return;

    // Check if a cancellation request is pending.
    if (!pending_cancellation)
        return;

    // Exit the thread via `pthread_exit`. This handles passing the
    // return value and calling the cleanup handlers for us.
    pthread_exit(PTHREAD_CANCELED);
}
#endif

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_cleanup_push.html
void pthread_cleanup_push(void (*routine)(void*), void* arg)
{
    cleanup_handlers.prepend({ routine, arg });
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_cleanup_pop.html
void pthread_cleanup_pop(int execute)
{
    VERIFY(!cleanup_handlers.is_empty());

    auto handler = cleanup_handlers.take_first();

    if (execute)
        handler.routine(handler.argument);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_join.html
int pthread_join(pthread_t thread, void** exit_value_ptr)
{
    __pthread_maybe_cancel();

    int rc = syscall(SC_join_thread, thread, exit_value_ptr);
    __RETURN_PTHREAD_ERROR(rc);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_kill.html
int pthread_kill(pthread_t thread, int sig)
{
    int rc = syscall(SC_kill_thread, thread, sig);
    __RETURN_PTHREAD_ERROR(rc);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_detach.html
int pthread_detach(pthread_t thread)
{
    int rc = syscall(SC_detach_thread, thread);
    __RETURN_PTHREAD_ERROR(rc);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_sigmask.html
int pthread_sigmask(int how, sigset_t const* set, sigset_t* old_set)
{
    if (sigprocmask(how, set, old_set))
        return errno;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_mutex_destroy.html
int pthread_mutex_destroy(pthread_mutex_t*)
{
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_mutexattr_init.html
int pthread_mutexattr_init(pthread_mutexattr_t* attr)
{
    attr->type = PTHREAD_MUTEX_NORMAL;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_mutexattr_destroy.html
int pthread_mutexattr_destroy(pthread_mutexattr_t*)
{
    return 0;
}

int pthread_mutexattr_settype(pthread_mutexattr_t* attr, int type)
{
    if (!attr)
        return EINVAL;
    if (type != PTHREAD_MUTEX_NORMAL && type != PTHREAD_MUTEX_RECURSIVE)
        return EINVAL;
    attr->type = type;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_mutexattr_gettype.html
int pthread_mutexattr_gettype(pthread_mutexattr_t* attr, int* type)
{
    *type = attr->type;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_init.html
int pthread_attr_init(pthread_attr_t* attributes)
{
    auto* impl = new PthreadAttrImpl {};
    *attributes = impl;

    dbgln_if(PTHREAD_DEBUG, "pthread_attr_init: New thread attributes at {}, detach state {}, priority {}, guard page size {}, stack size {}, stack location {}",
        impl,
        (PTHREAD_CREATE_JOINABLE == impl->detach_state) ? "joinable" : "detached",
        impl->schedule_priority,
        impl->guard_page_size,
        impl->stack_size,
        impl->stack_location);

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_destroy.html
int pthread_attr_destroy(pthread_attr_t* attributes)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));
    delete attributes_impl;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_getdetachstate.html
int pthread_attr_getdetachstate(pthread_attr_t const* attributes, int* p_detach_state)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl const* const*>(attributes));

    if (!attributes_impl || !p_detach_state)
        return EINVAL;

    *p_detach_state = attributes_impl->detach_state;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_setdetachstate.html
int pthread_attr_setdetachstate(pthread_attr_t* attributes, int detach_state)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));

    if (!attributes_impl)
        return EINVAL;

    if (detach_state != PTHREAD_CREATE_JOINABLE && detach_state != PTHREAD_CREATE_DETACHED)
        return EINVAL;

    attributes_impl->detach_state = detach_state;

    dbgln_if(PTHREAD_DEBUG, "pthread_attr_setdetachstate: Thread attributes at {}, detach state {}, priority {}, guard page size {}, stack size {}, stack location {}",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->detach_state) ? "joinable" : "detached",
        attributes_impl->schedule_priority,
        attributes_impl->guard_page_size,
        attributes_impl->stack_size,
        attributes_impl->stack_location);

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_getguardsize.html
int pthread_attr_getguardsize(pthread_attr_t const* attributes, size_t* p_guard_size)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl const* const*>(attributes));

    if (!attributes_impl || !p_guard_size)
        return EINVAL;

    *p_guard_size = attributes_impl->reported_guard_page_size;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_setguardsize.html
int pthread_attr_setguardsize(pthread_attr_t* attributes, size_t guard_size)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));

    if (!attributes_impl)
        return EINVAL;

    size_t actual_guard_size = guard_size;
    // round up
    if (0 != (guard_size % PAGE_SIZE))
        actual_guard_size += PAGE_SIZE - (guard_size % PAGE_SIZE);

    // what is the user even doing?
    if (actual_guard_size > highest_reasonable_guard_size) {
        return EINVAL;
    }

    attributes_impl->guard_page_size = actual_guard_size;
    attributes_impl->reported_guard_page_size = guard_size; // POSIX, why?

    dbgln_if(PTHREAD_DEBUG, "pthread_attr_setguardsize: Thread attributes at {}, detach state {}, priority {}, guard page size {}, stack size {}, stack location {}",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->detach_state) ? "joinable" : "detached",
        attributes_impl->schedule_priority,
        attributes_impl->guard_page_size,
        attributes_impl->stack_size,
        attributes_impl->stack_location);

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_getschedparam.html
int pthread_attr_getschedparam(pthread_attr_t const* attributes, struct sched_param* p_sched_param)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl const* const*>(attributes));

    if (!attributes_impl || !p_sched_param)
        return EINVAL;

    p_sched_param->sched_priority = attributes_impl->schedule_priority;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_setschedparam.html
int pthread_attr_setschedparam(pthread_attr_t* attributes, const struct sched_param* p_sched_param)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));
    if (!attributes_impl || !p_sched_param)
        return EINVAL;

    if (p_sched_param->sched_priority < THREAD_PRIORITY_MIN || p_sched_param->sched_priority > THREAD_PRIORITY_MAX)
        return ENOTSUP;

    attributes_impl->schedule_priority = p_sched_param->sched_priority;

    dbgln_if(PTHREAD_DEBUG, "pthread_attr_setschedparam: Thread attributes at {}, detach state {}, priority {}, guard page size {}, stack size {}, stack location {}",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->detach_state) ? "joinable" : "detached",
        attributes_impl->schedule_priority,
        attributes_impl->guard_page_size,
        attributes_impl->stack_size,
        attributes_impl->stack_location);

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_getstack.html
int pthread_attr_getstack(pthread_attr_t const* attributes, void** p_stack_ptr, size_t* p_stack_size)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl const* const*>(attributes));

    if (!attributes_impl || !p_stack_ptr || !p_stack_size)
        return EINVAL;

    *p_stack_ptr = attributes_impl->stack_location;
    *p_stack_size = attributes_impl->stack_size;

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_setstack.html
int pthread_attr_setstack(pthread_attr_t* attributes, void* p_stack, size_t stack_size)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));

    if (!attributes_impl || !p_stack)
        return EINVAL;

    // Check for required alignment on size
    if (0 != (stack_size % required_stack_alignment))
        return EINVAL;

    // FIXME: Check for required alignment on pointer?

    // FIXME: "[EACCES] The stack page(s) described by stackaddr and stacksize are not both readable and writable by the thread."
    // Have to check that the whole range is mapped to this process/thread? Can we defer this to create_thread?

    attributes_impl->stack_size = stack_size;
    attributes_impl->stack_location = p_stack;

    dbgln_if(PTHREAD_DEBUG, "pthread_attr_setstack: Thread attributes at {}, detach state {}, priority {}, guard page size {}, stack size {}, stack location {}",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->detach_state) ? "joinable" : "detached",
        attributes_impl->schedule_priority,
        attributes_impl->guard_page_size,
        attributes_impl->stack_size,
        attributes_impl->stack_location);

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_getstacksize.html
int pthread_attr_getstacksize(pthread_attr_t const* attributes, size_t* p_stack_size)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl const* const*>(attributes));

    if (!attributes_impl || !p_stack_size)
        return EINVAL;

    *p_stack_size = attributes_impl->stack_size;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_setstacksize.html
int pthread_attr_setstacksize(pthread_attr_t* attributes, size_t stack_size)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));

    if (!attributes_impl)
        return EINVAL;

    if (stack_size < PTHREAD_STACK_MIN || stack_size > PTHREAD_STACK_MAX)
        return EINVAL;

    attributes_impl->stack_size = stack_size;

    dbgln_if(PTHREAD_DEBUG, "pthread_attr_setstacksize: Thread attributes at {}, detach state {}, priority {}, guard page size {}, stack size {}, stack location {}",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->detach_state) ? "joinable" : "detached",
        attributes_impl->schedule_priority,
        attributes_impl->guard_page_size,
        attributes_impl->stack_size,
        attributes_impl->stack_location);

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_getscope.html
int pthread_attr_getscope([[maybe_unused]] pthread_attr_t const* attributes, [[maybe_unused]] int* contention_scope)
{
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_attr_setscope.html
int pthread_attr_setscope([[maybe_unused]] pthread_attr_t* attributes, [[maybe_unused]] int contention_scope)
{
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_getschedparam.html
int pthread_getschedparam(pthread_t thread, [[maybe_unused]] int* policy, struct sched_param* param)
{
    Syscall::SC_scheduler_parameters_params parameters {
        .pid_or_tid = thread,
        .mode = Syscall::SchedulerParametersMode::Thread,
        .parameters = *param,
    };
    int rc = syscall(Syscall::SC_scheduler_get_parameters, &parameters);
    if (rc == 0)
        *param = parameters.parameters;

    __RETURN_PTHREAD_ERROR(rc);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_setschedparam.html
int pthread_setschedparam(pthread_t thread, [[maybe_unused]] int policy, struct sched_param const* param)
{
    Syscall::SC_scheduler_parameters_params parameters {
        .pid_or_tid = thread,
        .mode = Syscall::SchedulerParametersMode::Thread,
        .parameters = *param,
    };
    int rc = syscall(Syscall::SC_scheduler_set_parameters, &parameters);
    __RETURN_PTHREAD_ERROR(rc);
}

static void pthread_cancel_signal_handler(int signal)
{
    // SIGCANCEL is a custom signal that is beyond the usual range of signal numbers.
    // Let's make sure we know about it in case we still end up in here, but the signal
    // number is being mangled.
    VERIFY(signal == SIGCANCEL);

    // Note: We don't handle PTHREAD_CANCEL_ASYNCHRONOUS any different from PTHREAD_CANCEL_DEFERRED,
    // since ASYNCHRONOUS just means that the thread can be cancelled at any time (instead of just
    // at the next cancellation point) and it seems to be generally discouraged to use it at all.
    pending_cancellation = true;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_cancel.html
// NOTE: libgcc expects this function to exist in libpthread, even if it is not implemented.
int pthread_cancel(pthread_t thread)
{
    // Set up our signal handler, which listens on SIGCANCEL and flips the cancellation indicator.
    // Note that signal handlers are shared across the whole process, so we can just set that up at any time.
    static bool set_up_cancel_handler = false;

    if (!set_up_cancel_handler) {
        struct sigaction act = {};
        act.sa_handler = pthread_cancel_signal_handler;
        sigaction(SIGCANCEL, &act, nullptr);
        set_up_cancel_handler = true;
    }

    return pthread_kill(thread, SIGCANCEL);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_testcancel.html
void pthread_testcancel(void)
{
    __pthread_maybe_cancel();
}

int pthread_setname_np(pthread_t thread, char const* name)
{
    if (!name)
        return EFAULT;
    int rc = prctl(PR_SET_THREAD_NAME, thread, name, strlen(name));
    __RETURN_PTHREAD_ERROR(rc);
}

int pthread_getname_np(pthread_t thread, char* buffer, size_t buffer_size)
{
    int rc = prctl(PR_GET_THREAD_NAME, thread, buffer, buffer_size);
    __RETURN_PTHREAD_ERROR(rc);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_setcancelstate.html
int pthread_setcancelstate(int state, int* oldstate)
{
    if (state != PTHREAD_CANCEL_ENABLE && state != PTHREAD_CANCEL_DISABLE)
        return EINVAL;
    if (oldstate)
        *oldstate = s_thread_cancel_state;
    s_thread_cancel_state = state;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_setcanceltype.html
int pthread_setcanceltype(int type, int* oldtype)
{
    if (type != PTHREAD_CANCEL_DEFERRED && type != PTHREAD_CANCEL_ASYNCHRONOUS)
        return EINVAL;
    if (oldtype)
        *oldtype = s_thread_cancel_type;
    s_thread_cancel_type = type;
    return 0;
}

constexpr static pid_t spinlock_unlock_sentinel = 0;
// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_spin_destroy.html
int pthread_spin_destroy(pthread_spinlock_t* lock)
{
    auto current = AK::atomic_load(&lock->m_lock);

    if (current != spinlock_unlock_sentinel)
        return EBUSY;

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_spin_init.html
int pthread_spin_init(pthread_spinlock_t* lock, [[maybe_unused]] int shared)
{
    lock->m_lock = spinlock_unlock_sentinel;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_spin_lock.html
int pthread_spin_lock(pthread_spinlock_t* lock)
{
    auto const desired = gettid();
    while (true) {
        auto current = AK::atomic_load(&lock->m_lock);

        if (current == desired)
            return EDEADLK;

        if (AK::atomic_compare_exchange_strong(&lock->m_lock, current, desired, AK::MemoryOrder::memory_order_acquire))
            break;
    }

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_spin_trylock.html
int pthread_spin_trylock(pthread_spinlock_t* lock)
{
    // We expect the current value to be unlocked, as the specification
    // states that trylock should lock only if it is not held by ANY thread.
    auto current = spinlock_unlock_sentinel;
    auto desired = gettid();

    if (AK::atomic_compare_exchange_strong(&lock->m_lock, current, desired, AK::MemoryOrder::memory_order_acquire)) {
        return 0;
    } else {
        return EBUSY;
    }
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_spin_unlock.html
int pthread_spin_unlock(pthread_spinlock_t* lock)
{
    auto current = AK::atomic_load(&lock->m_lock);

    if (gettid() != current)
        return EPERM;

    AK::atomic_store(&lock->m_lock, spinlock_unlock_sentinel);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_equal.html
int pthread_equal(pthread_t t1, pthread_t t2)
{
    return t1 == t2;
}

// FIXME: Use the fancy futex mechanism above to write an rw lock.
//        For the time being, let's just use a less-than-good lock to get things working.
// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlock_destroy.html
int pthread_rwlock_destroy(pthread_rwlock_t* rl)
{
    if (!rl)
        return 0;
    return 0;
}

// In a very non-straightforward way, this value is composed of two 32-bit integers
// the top 32 bits are reserved for the ID of write-locking thread (if any)
// and the bottom 32 bits are:
//     top 2 bits (30,31): reader wake mask, writer wake mask
//     middle 16 bits: information
//        bit 16: someone is waiting to write
//        bit 17: locked for write
//     bottom 16 bits (0..15): reader count
constexpr static u32 reader_wake_mask = 1 << 30;
constexpr static u32 writer_wake_mask = 1 << 31;
constexpr static u32 writer_locked_mask = 1 << 17;
constexpr static u32 writer_intent_mask = 1 << 16;
// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlock_init.html
int pthread_rwlock_init(pthread_rwlock_t* __restrict lockp, pthread_rwlockattr_t const* __restrict attr)
{
    // Just ignore the attributes. use defaults for now.
    (void)attr;

    // No readers, no writer, not locked at all.
    *lockp = 0;
    return 0;
}

// Note that this function does not care about the top 32 bits at all.
static int rwlock_rdlock_maybe_timed(u32* lockp, const struct timespec* timeout = nullptr, bool only_once = false, int value_if_timeout = -1, int value_if_okay = -2)
{
    auto current = AK::atomic_load(lockp);
    for (; !only_once;) {
        // First, see if this is locked for writing
        // if it's not, try to add to the counter.
        // If someone is waiting to write, and there is one or no other readers, let them have the lock.
        if (!(current & writer_locked_mask)) {
            auto count = (u16)current;
            if (!(current & writer_intent_mask) || count > 1) {
                ++count;
                auto desired = (current & 0xffff0000u) | count;
                auto did_exchange = AK::atomic_compare_exchange_strong(lockp, current, desired, AK::MemoryOrder::memory_order_acquire);
                if (!did_exchange)
                    continue; // tough luck, try again.
                return value_if_okay;
            }
        }

        // If no one else is waiting for the read wake bit, set it.
        if (!(current & reader_wake_mask)) {
            auto desired = current | reader_wake_mask;
            auto did_exchange = AK::atomic_compare_exchange_strong(lockp, current, desired, AK::MemoryOrder::memory_order_acquire);
            if (!did_exchange)
                continue; // Something interesting happened!

            current = desired;
        }

        // Seems like someone is writing (or is interested in writing and we let them have the lock)
        // wait until they're done.
        auto rc = futex(lockp, FUTEX_WAIT_BITSET | FUTEX_PRIVATE_FLAG, current, timeout, nullptr, reader_wake_mask);
        if (rc < 0 && errno == ETIMEDOUT && timeout) {
            return value_if_timeout;
        }
        if (rc < 0 && errno != EAGAIN) {
            // Something broke. let's just bail out.
            return errno;
        }
        errno = 0;
        // Reload the 'current' value
        current = AK::atomic_load(lockp);
    }
    return value_if_timeout;
}

static int rwlock_wrlock_maybe_timed(pthread_rwlock_t* lockval_p, const struct timespec* timeout = nullptr, bool only_once = false, int value_if_timeout = -1, int value_if_okay = -2)
{
    u32* lockp = reinterpret_cast<u32*>(lockval_p);
    auto current = AK::atomic_load(lockp);
    for (; !only_once;) {
        // First, see if this is locked for writing, and if there are any readers.
        // if not, lock it.
        // If someone is waiting to write, let them have the lock.
        if (!(current & writer_locked_mask) && ((u16)current) == 0) {
            if (!(current & writer_intent_mask)) {
                auto desired = current | writer_locked_mask | writer_intent_mask;
                auto did_exchange = AK::atomic_compare_exchange_strong(lockp, current, desired, AK::MemoryOrder::memory_order_acquire);
                if (!did_exchange)
                    continue;

                // Now that we've locked the value, it's safe to set our thread ID.
                AK::atomic_store(reinterpret_cast<i32*>(lockval_p) + 1, pthread_self());
                return value_if_okay;
            }
        }

        // That didn't work, if no one else is waiting for the write bit, set it.
        if (!(current & writer_wake_mask)) {
            auto desired = current | writer_wake_mask | writer_intent_mask;
            auto did_exchange = AK::atomic_compare_exchange_strong(lockp, current, desired, AK::MemoryOrder::memory_order_acquire);
            if (!did_exchange)
                continue; // Something interesting happened!

            current = desired;
        }

        // Seems like someone is writing (or is interested in writing and we let them have the lock)
        // wait until they're done.
        auto rc = futex(lockp, FUTEX_WAIT_BITSET | FUTEX_PRIVATE_FLAG, current, timeout, nullptr, writer_wake_mask);
        if (rc < 0 && errno == ETIMEDOUT && timeout) {
            return value_if_timeout;
        }
        if (rc < 0 && errno != EAGAIN) {
            // Something broke. let's just bail out.
            return errno;
        }
        errno = 0;
        // Reload the 'current' value
        current = AK::atomic_load(lockp);
    }

    return value_if_timeout;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlock_rdlock.html
int pthread_rwlock_rdlock(pthread_rwlock_t* lockp)
{
    if (!lockp)
        return EINVAL;

    return rwlock_rdlock_maybe_timed(reinterpret_cast<u32*>(lockp), nullptr, false, 0, 0);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlock_timedrdlock.html
int pthread_rwlock_timedrdlock(pthread_rwlock_t* __restrict lockp, const struct timespec* __restrict timespec)
{
    if (!lockp)
        return EINVAL;

    auto rc = rwlock_rdlock_maybe_timed(reinterpret_cast<u32*>(lockp), timespec);
    if (rc == -1) // "ok"
        return 0;
    if (rc == -2) // "timed out"
        return 1;
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlock_timedwrlock.html
int pthread_rwlock_timedwrlock(pthread_rwlock_t* __restrict lockp, const struct timespec* __restrict timespec)
{
    if (!lockp)
        return EINVAL;

    auto rc = rwlock_wrlock_maybe_timed(lockp, timespec);
    if (rc == -1) // "ok"
        return 0;
    if (rc == -2) // "timed out"
        return 1;
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlock_tryrdlock.html
int pthread_rwlock_tryrdlock(pthread_rwlock_t* lockp)
{
    if (!lockp)
        return EINVAL;

    return rwlock_rdlock_maybe_timed(reinterpret_cast<u32*>(lockp), nullptr, true, EBUSY, 0);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlock_trywrlock.html
int pthread_rwlock_trywrlock(pthread_rwlock_t* lockp)
{
    if (!lockp)
        return EINVAL;

    return rwlock_wrlock_maybe_timed(lockp, nullptr, true, EBUSY, 0);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlock_unlock.html
int pthread_rwlock_unlock(pthread_rwlock_t* lockval_p)
{
    if (!lockval_p)
        return EINVAL;

    // This is a weird API, we don't really know whether we're unlocking write or read...
    auto lockp = reinterpret_cast<u32*>(lockval_p);
    auto current = AK::atomic_load(lockp, AK::MemoryOrder::memory_order_relaxed);
    if (current & writer_locked_mask) {
        // If this lock is locked for writing, its owner better be us!
        auto owner_id = AK::atomic_load(reinterpret_cast<i32*>(lockval_p) + 1);
        auto my_id = pthread_self();
        if (owner_id != my_id)
            return EINVAL; // you don't own this lock, silly.

        // Now just unlock it.
        auto desired = current & ~(writer_locked_mask | writer_intent_mask);
        AK::atomic_store(lockp, desired, AK::MemoryOrder::memory_order_release);
        // Then wake both readers and writers, if any.
        auto rc = futex(lockp, FUTEX_WAKE_BITSET | FUTEX_PRIVATE_FLAG, UINT32_MAX, nullptr, nullptr, (current & writer_wake_mask) | reader_wake_mask);
        if (rc < 0)
            return errno;
        return 0;
    }

    for (;;) {
        auto count = (u16)current;
        if (!count) {
            // Are you crazy? this isn't even locked!
            return EINVAL;
        }
        --count;
        auto desired = (current & 0xffff0000u) | count;
        auto did_exchange = AK::atomic_compare_exchange_strong(lockp, current, desired, AK::MemoryOrder::memory_order_release);
        if (did_exchange)
            break;
        // tough luck, try again.
    }

    // Finally, unlocked at last!
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlock_wrlock.html
int pthread_rwlock_wrlock(pthread_rwlock_t* lockp)
{
    if (!lockp)
        return EINVAL;

    return rwlock_wrlock_maybe_timed(lockp, nullptr, false, 0, 0);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlockattr_destroy.html
int pthread_rwlockattr_destroy(pthread_rwlockattr_t*)
{
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlockattr_getpshared.html
int pthread_rwlockattr_getpshared(pthread_rwlockattr_t const* __restrict, int* __restrict)
{
    VERIFY_NOT_REACHED();
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlockattr_init.html
int pthread_rwlockattr_init(pthread_rwlockattr_t*)
{
    VERIFY_NOT_REACHED();
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_rwlockattr_setpshared.html
int pthread_rwlockattr_setpshared(pthread_rwlockattr_t*, int)
{
    VERIFY_NOT_REACHED();
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_atfork.html
int pthread_atfork(void (*prepare)(void), void (*parent)(void), void (*child)(void))
{
    if (prepare)
        __pthread_fork_atfork_register_prepare(prepare);
    if (parent)
        __pthread_fork_atfork_register_parent(parent);
    if (child)
        __pthread_fork_atfork_register_child(child);
    return 0;
}

} // extern "C"
