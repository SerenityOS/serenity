/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Userspace.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$ioctl(int fd, unsigned request, FlatPtr arg)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto description = TRY(open_file_description(fd));
    if (request == FIONBIO) {
        description->set_blocking(TRY(copy_typed_from_user(Userspace<int const*>(arg))) == 0);
        return 0;
    }
    if (request == FIOCLEX) {
        m_fds.with_exclusive([&](auto& fds) {
            fds[fd].set_flags(fds[fd].flags() | FD_CLOEXEC);
        });
        return 0;
    }
    if (request == FIONCLEX) {
        m_fds.with_exclusive([&](auto& fds) {
            fds[fd].set_flags(fds[fd].flags() & ~FD_CLOEXEC);
        });
        return 0;
    }
    TRY(description->file().ioctl(*description, request, arg));
    return 0;
}

}
