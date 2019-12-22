#include <Kernel/Syscall.h>
#include <errno.h>
#include <serenity.h>

extern "C" {

int module_load(const char* path, size_t path_length)
{
    int rc = syscall(SC_module_load, path, path_length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int module_unload(const char* name, size_t name_length)
{
    int rc = syscall(SC_module_unload, name, name_length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int profiling_enable(pid_t pid)
{
    int rc = syscall(SC_profiling_enable, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int profiling_disable(pid_t pid)
{
    int rc = syscall(SC_profiling_disable, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int futex(int32_t* userspace_address, int futex_op, int32_t value, const struct timespec* timeout)
{
    Syscall::SC_futex_params params { userspace_address, futex_op, value, timeout };
    int rc = syscall(SC_futex, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}
