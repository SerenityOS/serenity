#include <AK/StdLibExtras.h>
#include <pthread.h>
#include <unistd.h>

extern "C" {

int pthread_create(pthread_t* thread, pthread_attr_t* attributes, void *(*start_routine)(void*), void* argument_to_start_routine)
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

}
