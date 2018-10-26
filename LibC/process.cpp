#include "process.h"
#include "errno.h"
#include <Kernel/Syscall.h>

extern "C" {

int spawn(const char* path, const char** args)
{
    int rc = Syscall::invoke(Syscall::Spawn, (dword)path, (dword)args);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

