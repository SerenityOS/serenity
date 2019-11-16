#include <AK/Atomic.h>
#include <AK/StdLibExtras.h>
#include <Kernel/Syscall.h>
#include <pthread.h>
#include <unistd.h>
#include <stdio.h>

extern "C" {

int pthread_create(pthread_t* thread, pthread_attr_t* attributes, void* (*start_routine)(void*), void* argument_to_start_routine)
{
    if (!thread)
        return -EINVAL;
    UNUSED_PARAM(attributes);
    int rc = create_thread(start_routine, argument_to_start_routine);
    if (rc < 0)
        return rc;
    *thread = rc;
    return 0;
}

void pthread_exit(void* value_ptr)
{
    exit_thread(value_ptr);
}

int pthread_join(pthread_t thread, void** exit_value_ptr)
{
    int rc = syscall(SC_join_thread, thread, exit_value_ptr);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int pthread_mutex_init(pthread_mutex_t* mutex, const pthread_mutexattr_t* attributes)
{
    // FIXME: Implement mutex attributes
    UNUSED_PARAM(attributes);
    *mutex = 0;
    return 0;
}

int pthread_mutex_lock(pthread_mutex_t* mutex)
{
    auto* atomic = reinterpret_cast<Atomic<u32>*>(mutex);
    for (;;) {
        u32 expected = false;
        if (atomic->compare_exchange_strong(expected, true, AK::memory_order_acq_rel))
            return 0;
        sched_yield();
    }
}

int pthread_mutex_unlock(pthread_mutex_t* mutex)
{
    auto* atomic = reinterpret_cast<Atomic<u32>*>(mutex);
    atomic->store(false, AK::memory_order_release);
    return 0;
}

}
