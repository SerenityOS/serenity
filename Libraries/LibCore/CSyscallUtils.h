#pragma once

#include <AK/LogStream.h>
#include <AK/StdLibExtras.h>
#include <errno.h>
#include <sys/types.h>
#include <unistd.h>

namespace CSyscallUtils {

template<typename Syscall, class... Args>
inline int safe_syscall(Syscall syscall, Args&&... args)
{
    for (;;) {
        int sysret = syscall(forward<Args>(args)...);
        if (sysret == -1) {
            int saved_errno = errno;
            dbg() << "CSafeSyscall: " << sysret << " (" << saved_errno << ": " << strerror(saved_errno) << ")";
            if (errno == EINTR)
                continue;
            ASSERT_NOT_REACHED();
        }
        return sysret;
    }
}

}
