/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/StdLibExtras.h>
#include <Kernel/API/Syscall.h>
#include <limits.h>
#include <pthread.h>
#include <serenity.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>

namespace {
using PthreadAttrImpl = Syscall::SC_create_thread_params;

struct KeyDestroyer {
    ~KeyDestroyer() { destroy_for_current_thread(); }
    static void destroy_for_current_thread();
};

} // end anonymous namespace

constexpr size_t required_stack_alignment = 4 * MiB;
constexpr size_t highest_reasonable_guard_size = 32 * PAGE_SIZE;
constexpr size_t highest_reasonable_stack_size = 8 * MiB; // That's the default in Ubuntu?

// Create an RAII object with a global destructor to destroy pthread keys for the main thread.
// Impact of this: Any global object that wants to do something with pthread_getspecific
// in its destructor from the main thread might be in for a nasty surprise.
static KeyDestroyer s_key_destroyer;

#define __RETURN_PTHREAD_ERROR(rc) \
    return ((rc) < 0 ? -(rc) : 0)

extern "C" {

static void* pthread_create_helper(void* (*routine)(void*), void* argument)
{
    void* ret_val = routine(argument);
    pthread_exit(ret_val);
    return nullptr;
}

static int create_thread(pthread_t* thread, void* (*entry)(void*), void* argument, PthreadAttrImpl* thread_params)
{
    void** stack = (void**)((uintptr_t)thread_params->m_stack_location + thread_params->m_stack_size);

    auto push_on_stack = [&](void* data) {
        stack--;
        *stack = data;
        thread_params->m_stack_size -= sizeof(void*);
    };

    // We set up the stack for pthread_create_helper.
    // Note that we need to align the stack to 16B, accounting for
    // the fact that we also push 8 bytes.
    while (((uintptr_t)stack - 8) % 16 != 0)
        push_on_stack(nullptr);

    push_on_stack(argument);
    push_on_stack((void*)entry);
    ASSERT((uintptr_t)stack % 16 == 0);

    // Push a fake return address
    push_on_stack(nullptr);

    int rc = syscall(SC_create_thread, pthread_create_helper, thread_params);
    if (rc >= 0)
        *thread = rc;
    __RETURN_PTHREAD_ERROR(rc);
}

[[noreturn]] static void exit_thread(void* code)
{
    KeyDestroyer::destroy_for_current_thread();
    syscall(SC_exit_thread, code);
    ASSERT_NOT_REACHED();
}

int pthread_self()
{
    return gettid();
}

int pthread_create(pthread_t* thread, pthread_attr_t* attributes, void* (*start_routine)(void*), void* argument_to_start_routine)
{
    if (!thread)
        return -EINVAL;

    PthreadAttrImpl default_attributes {};
    PthreadAttrImpl** arg_attributes = reinterpret_cast<PthreadAttrImpl**>(attributes);

    PthreadAttrImpl* used_attributes = arg_attributes ? *arg_attributes : &default_attributes;

    if (!used_attributes->m_stack_location) {
        // adjust stack size, user might have called setstacksize, which has no restrictions on size/alignment
        if (0 != (used_attributes->m_stack_size % required_stack_alignment))
            used_attributes->m_stack_size += required_stack_alignment - (used_attributes->m_stack_size % required_stack_alignment);

        used_attributes->m_stack_location = mmap_with_name(nullptr, used_attributes->m_stack_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS | MAP_STACK, 0, 0, "Thread stack");
        if (!used_attributes->m_stack_location)
            return -1;
    }

#ifdef PTHREAD_DEBUG
    dbgprintf("pthread_create: Creating thread with attributes at %p, detach state %s, priority %d, guard page size %d, stack size %d, stack location %p\n",
        used_attributes,
        (PTHREAD_CREATE_JOINABLE == used_attributes->m_detach_state) ? "joinable" : "detached",
        used_attributes->m_schedule_priority,
        used_attributes->m_guard_page_size,
        used_attributes->m_stack_size,
        used_attributes->m_stack_location);
#endif

    return create_thread(thread, start_routine, argument_to_start_routine, used_attributes);
}

void pthread_exit(void* value_ptr)
{
    exit_thread(value_ptr);
}

int pthread_join(pthread_t thread, void** exit_value_ptr)
{
    int rc = syscall(SC_join_thread, thread, exit_value_ptr);
    __RETURN_PTHREAD_ERROR(rc);
}

int pthread_detach(pthread_t thread)
{
    int rc = syscall(SC_detach_thread, thread);
    __RETURN_PTHREAD_ERROR(rc);
}

int pthread_sigmask(int how, const sigset_t* set, sigset_t* old_set)
{
    if (sigprocmask(how, set, old_set))
        return errno;
    return 0;
}

int pthread_mutex_init(pthread_mutex_t* mutex, const pthread_mutexattr_t* attributes)
{
    mutex->lock = 0;
    mutex->owner = 0;
    mutex->level = 0;
    mutex->type = attributes ? attributes->type : PTHREAD_MUTEX_NORMAL;
    return 0;
}

int pthread_mutex_destroy(pthread_mutex_t*)
{
    return 0;
}

int pthread_mutex_lock(pthread_mutex_t* mutex)
{
    auto& atomic = reinterpret_cast<Atomic<u32>&>(mutex->lock);
    pthread_t this_thread = pthread_self();
    for (;;) {
        u32 expected = false;
        if (!atomic.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            if (mutex->type == PTHREAD_MUTEX_RECURSIVE && mutex->owner == this_thread) {
                mutex->level++;
                return 0;
            }
            sched_yield();
            continue;
        }
        mutex->owner = this_thread;
        mutex->level = 0;
        return 0;
    }
}

int pthread_mutex_trylock(pthread_mutex_t* mutex)
{
    auto& atomic = reinterpret_cast<Atomic<u32>&>(mutex->lock);
    u32 expected = false;
    if (!atomic.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
        if (mutex->type == PTHREAD_MUTEX_RECURSIVE && mutex->owner == pthread_self()) {
            mutex->level++;
            return 0;
        }
        return EBUSY;
    }
    mutex->owner = pthread_self();
    mutex->level = 0;
    return 0;
}

int pthread_mutex_unlock(pthread_mutex_t* mutex)
{
    if (mutex->type == PTHREAD_MUTEX_RECURSIVE && mutex->level > 0) {
        mutex->level--;
        return 0;
    }
    mutex->owner = 0;
    mutex->lock = 0;
    return 0;
}

int pthread_mutexattr_init(pthread_mutexattr_t* attr)
{
    attr->type = PTHREAD_MUTEX_NORMAL;
    return 0;
}

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

int pthread_attr_init(pthread_attr_t* attributes)
{
    auto* impl = new PthreadAttrImpl {};
    *attributes = impl;

#ifdef PTHREAD_DEBUG
    dbgprintf("pthread_attr_init: New thread attributes at %p, detach state %s, priority %d, guard page size %d, stack size %d, stack location %p\n",
        impl,
        (PTHREAD_CREATE_JOINABLE == impl->m_detach_state) ? "joinable" : "detached",
        impl->m_schedule_priority,
        impl->m_guard_page_size,
        impl->m_stack_size,
        impl->m_stack_location);
#endif

    return 0;
}

int pthread_attr_destroy(pthread_attr_t* attributes)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));
    delete attributes_impl;
    return 0;
}

int pthread_attr_getdetachstate(const pthread_attr_t* attributes, int* p_detach_state)
{
    auto* attributes_impl = *(reinterpret_cast<const PthreadAttrImpl* const*>(attributes));

    if (!attributes_impl || !p_detach_state)
        return EINVAL;

    *p_detach_state = attributes_impl->m_detach_state;
    return 0;
}

int pthread_attr_setdetachstate(pthread_attr_t* attributes, int detach_state)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));

    if (!attributes_impl)
        return EINVAL;

    if (detach_state != PTHREAD_CREATE_JOINABLE && detach_state != PTHREAD_CREATE_DETACHED)
        return EINVAL;

    attributes_impl->m_detach_state = detach_state;

#ifdef PTHREAD_DEBUG
    dbgprintf("pthread_attr_setdetachstate: Thread attributes at %p, detach state %s, priority %d, guard page size %d, stack size %d, stack location %p\n",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->m_detach_state) ? "joinable" : "detached",
        attributes_impl->m_schedule_priority,
        attributes_impl->m_guard_page_size,
        attributes_impl->m_stack_size,
        attributes_impl->m_stack_location);
#endif

    return 0;
}

int pthread_attr_getguardsize(const pthread_attr_t* attributes, size_t* p_guard_size)
{
    auto* attributes_impl = *(reinterpret_cast<const PthreadAttrImpl* const*>(attributes));

    if (!attributes_impl || !p_guard_size)
        return EINVAL;

    *p_guard_size = attributes_impl->m_reported_guard_page_size;
    return 0;
}

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

    attributes_impl->m_guard_page_size = actual_guard_size;
    attributes_impl->m_reported_guard_page_size = guard_size; // POSIX, why?

#ifdef PTHREAD_DEBUG
    dbgprintf("pthread_attr_setguardsize: Thread attributes at %p, detach state %s, priority %d, guard page size %d, stack size %d, stack location %p\n",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->m_detach_state) ? "joinable" : "detached",
        attributes_impl->m_schedule_priority,
        attributes_impl->m_guard_page_size,
        attributes_impl->m_stack_size,
        attributes_impl->m_stack_location);
#endif

    return 0;
}

int pthread_attr_getschedparam(const pthread_attr_t* attributes, struct sched_param* p_sched_param)
{
    auto* attributes_impl = *(reinterpret_cast<const PthreadAttrImpl* const*>(attributes));

    if (!attributes_impl || !p_sched_param)
        return EINVAL;

    p_sched_param->sched_priority = attributes_impl->m_schedule_priority;
    return 0;
}

int pthread_attr_setschedparam(pthread_attr_t* attributes, const struct sched_param* p_sched_param)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));
    if (!attributes_impl || !p_sched_param)
        return EINVAL;

    if (p_sched_param->sched_priority < THREAD_PRIORITY_MIN || p_sched_param->sched_priority > THREAD_PRIORITY_MAX)
        return ENOTSUP;

    attributes_impl->m_schedule_priority = p_sched_param->sched_priority;

#ifdef PTHREAD_DEBUG
    dbgprintf("pthread_attr_setschedparam: Thread attributes at %p, detach state %s, priority %d, guard page size %d, stack size %d, stack location %p\n",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->m_detach_state) ? "joinable" : "detached",
        attributes_impl->m_schedule_priority,
        attributes_impl->m_guard_page_size,
        attributes_impl->m_stack_size,
        attributes_impl->m_stack_location);
#endif

    return 0;
}

int pthread_attr_getstack(const pthread_attr_t* attributes, void** p_stack_ptr, size_t* p_stack_size)
{
    auto* attributes_impl = *(reinterpret_cast<const PthreadAttrImpl* const*>(attributes));

    if (!attributes_impl || !p_stack_ptr || !p_stack_size)
        return EINVAL;

    *p_stack_ptr = attributes_impl->m_stack_location;
    *p_stack_size = attributes_impl->m_stack_size;

    return 0;
}

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

    attributes_impl->m_stack_size = stack_size;
    attributes_impl->m_stack_location = p_stack;

#ifdef PTHREAD_DEBUG
    dbgprintf("pthread_attr_setstack: Thread attributes at %p, detach state %s, priority %d, guard page size %d, stack size %d, stack location %p\n",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->m_detach_state) ? "joinable" : "detached",
        attributes_impl->m_schedule_priority,
        attributes_impl->m_guard_page_size,
        attributes_impl->m_stack_size,
        attributes_impl->m_stack_location);
#endif

    return 0;
}

int pthread_attr_getstacksize(const pthread_attr_t* attributes, size_t* p_stack_size)
{
    auto* attributes_impl = *(reinterpret_cast<const PthreadAttrImpl* const*>(attributes));

    if (!attributes_impl || !p_stack_size)
        return EINVAL;

    *p_stack_size = attributes_impl->m_stack_size;
    return 0;
}

int pthread_attr_setstacksize(pthread_attr_t* attributes, size_t stack_size)
{
    auto* attributes_impl = *(reinterpret_cast<PthreadAttrImpl**>(attributes));

    if (!attributes_impl)
        return EINVAL;

    if ((stack_size < PTHREAD_STACK_MIN) || stack_size > highest_reasonable_stack_size)
        return EINVAL;

    attributes_impl->m_stack_size = stack_size;

#ifdef PTHREAD_DEBUG
    dbgprintf("pthread_attr_setstacksize: Thread attributes at %p, detach state %s, priority %d, guard page size %d, stack size %d, stack location %p\n",
        attributes_impl,
        (PTHREAD_CREATE_JOINABLE == attributes_impl->m_detach_state) ? "joinable" : "detached",
        attributes_impl->m_schedule_priority,
        attributes_impl->m_guard_page_size,
        attributes_impl->m_stack_size,
        attributes_impl->m_stack_location);
#endif

    return 0;
}

int pthread_getschedparam([[maybe_unused]] pthread_t thread, [[maybe_unused]] int* policy, [[maybe_unused]] struct sched_param* param)
{
    return 0;
}

int pthread_setschedparam([[maybe_unused]] pthread_t thread, [[maybe_unused]] int policy, [[maybe_unused]] const struct sched_param* param)
{
    return 0;
}

int pthread_cond_init(pthread_cond_t* cond, const pthread_condattr_t* attr)
{
    cond->value = 0;
    cond->previous = 0;
    cond->clockid = attr ? attr->clockid : CLOCK_MONOTONIC_COARSE;
    return 0;
}

int pthread_cond_destroy(pthread_cond_t*)
{
    return 0;
}

static int futex_wait(uint32_t& futex_addr, uint32_t value, const struct timespec* abstime)
{
    int saved_errno = errno;
    // NOTE: FUTEX_WAIT takes a relative timeout, so use FUTEX_WAIT_BITSET instead!
    int rc = futex(&futex_addr, FUTEX_WAIT_BITSET, value, abstime, nullptr, FUTEX_BITSET_MATCH_ANY);
    if (rc < 0 && errno == EAGAIN) {
        // If we didn't wait, that's not an error
        errno = saved_errno;
        rc = 0;
    }
    return rc;
}

static int cond_wait(pthread_cond_t* cond, pthread_mutex_t* mutex, const struct timespec* abstime)
{
    u32 value = cond->value;
    cond->previous = value;
    pthread_mutex_unlock(mutex);
    int rc = futex_wait(cond->value, value, abstime);
    pthread_mutex_lock(mutex);
    return rc;
}

int pthread_cond_wait(pthread_cond_t* cond, pthread_mutex_t* mutex)
{
    int rc = cond_wait(cond, mutex, nullptr);
    ASSERT(rc == 0);
    return 0;
}

int pthread_condattr_init(pthread_condattr_t* attr)
{
    attr->clockid = CLOCK_MONOTONIC_COARSE;
    return 0;
}

int pthread_condattr_destroy(pthread_condattr_t*)
{
    return 0;
}

int pthread_condattr_setclock(pthread_condattr_t* attr, clockid_t clock)
{
    attr->clockid = clock;
    return 0;
}

int pthread_cond_timedwait(pthread_cond_t* cond, pthread_mutex_t* mutex, const struct timespec* abstime)
{
    return cond_wait(cond, mutex, abstime);
}

int pthread_cond_signal(pthread_cond_t* cond)
{
    u32 value = cond->previous + 1;
    cond->value = value;
    int rc = futex(&cond->value, FUTEX_WAKE, 1, nullptr, nullptr, 0);
    ASSERT(rc >= 0);
    return 0;
}

int pthread_cond_broadcast(pthread_cond_t* cond)
{
    u32 value = cond->previous + 1;
    cond->value = value;
    int rc = futex(&cond->value, FUTEX_WAKE, INT32_MAX, nullptr, nullptr, 0);
    ASSERT(rc >= 0);
    return 0;
}

static constexpr int max_keys = PTHREAD_KEYS_MAX;

typedef void (*KeyDestructor)(void*);

struct KeyTable {
    KeyDestructor destructors[max_keys] { nullptr };
    int next { 0 };
    pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
};

struct SpecificTable {
    void* values[max_keys] { nullptr };
};

static KeyTable s_keys;

__thread SpecificTable t_specifics;

int pthread_key_create(pthread_key_t* key, KeyDestructor destructor)
{
    int ret = 0;
    pthread_mutex_lock(&s_keys.mutex);
    if (s_keys.next >= max_keys) {
        ret = EAGAIN;
    } else {
        *key = s_keys.next++;
        s_keys.destructors[*key] = destructor;
        ret = 0;
    }
    pthread_mutex_unlock(&s_keys.mutex);
    return ret;
}

int pthread_key_delete(pthread_key_t key)
{
    if (key < 0 || key >= max_keys)
        return EINVAL;
    pthread_mutex_lock(&s_keys.mutex);
    s_keys.destructors[key] = nullptr;
    pthread_mutex_unlock(&s_keys.mutex);
    return 0;
}

void* pthread_getspecific(pthread_key_t key)
{
    if (key < 0)
        return nullptr;
    if (key >= max_keys)
        return nullptr;
    return t_specifics.values[key];
}

int pthread_setspecific(pthread_key_t key, const void* value)
{
    if (key < 0)
        return EINVAL;
    if (key >= max_keys)
        return EINVAL;

    t_specifics.values[key] = const_cast<void*>(value);
    return 0;
}

void KeyDestroyer::destroy_for_current_thread()
{
    // This function will either be called during exit_thread, for a pthread, or
    // during global program shutdown for the main thread.

    pthread_mutex_lock(&s_keys.mutex);
    size_t num_used_keys = s_keys.next;

    // Dr. POSIX accounts for weird key destructors setting their own key again.
    // Or even, setting other unrelated keys? Odd, but whatever the Doc says goes.

    for (size_t destruct_iteration = 0; destruct_iteration < PTHREAD_DESTRUCTOR_ITERATIONS; ++destruct_iteration) {
        bool any_nonnull_destructors = false;
        for (size_t key_index = 0; key_index < num_used_keys; ++key_index) {
            void* value = exchange(t_specifics.values[key_index], nullptr);

            if (value && s_keys.destructors[key_index]) {
                any_nonnull_destructors = true;
                (*s_keys.destructors[key_index])(value);
            }
        }
        if (!any_nonnull_destructors)
            break;
    }
    pthread_mutex_unlock(&s_keys.mutex);
}

int pthread_setname_np(pthread_t thread, const char* name)
{
    if (!name)
        return EFAULT;
    int rc = syscall(SC_set_thread_name, thread, name, strlen(name));
    __RETURN_PTHREAD_ERROR(rc);
}

int pthread_getname_np(pthread_t thread, char* buffer, size_t buffer_size)
{
    int rc = syscall(SC_get_thread_name, thread, buffer, buffer_size);
    __RETURN_PTHREAD_ERROR(rc);
}

int pthread_setcancelstate([[maybe_unused]] int state, [[maybe_unused]] int* oldstate)
{
    TODO();
}

int pthread_setcanceltype([[maybe_unused]] int type, [[maybe_unused]] int* oldtype)
{
    TODO();
}

int pthread_equal(pthread_t t1, pthread_t t2)
{
    return t1 == t2;
}

} // extern "C"
