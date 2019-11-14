#include <AK/StdLibExtras.h>
#include <Kernel/Syscall.h>
#include <pthread.h>
#include <unistd.h>

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
}
