#pragma once

#include <errno.h>
#include <unistd.h>
#include <sys/types.h>
#include <AK/StdLibExtras.h>

namespace CSyscallUtils {

template <typename Syscall, class... Args>
inline int safe_syscall(Syscall syscall, Args&& ... args) {
    for (;;) {
        int sysret = syscall(forward<Args>(args)...);
        if (sysret == -1) {
            dbgprintf("CSafeSyscall: %d (%d: %s)\n", sysret, errno, strerror(errno));
            if (errno == EINTR)
                continue;
            ASSERT_NOT_REACHED();
        }
        return sysret;
    }
}

}

