#include "process.h"
#include <Kernel/Syscall.h>

extern "C" {

int spawn(const char* path)
{
    return Syscall::invoke(Syscall::Spawn, (dword)path);
}

}

