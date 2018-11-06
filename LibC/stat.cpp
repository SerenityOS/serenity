#include <sys/stat.h>
#include <Kernel/Syscall.h>

extern "C" {

mode_t umask(mode_t mask)
{
    return Syscall::invoke(Syscall::SC_umask, (dword)mask);
}

}

