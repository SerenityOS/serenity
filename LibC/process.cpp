#include "process.h"
#include "errno.h"
#include <Kernel/Syscall.h>

extern "C" {

int spawn(const char* path, const char** args, const char** envp)
{
    int rc = Syscall::invoke(Syscall::SC_spawn, (dword)path, (dword)args, (dword)envp);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

