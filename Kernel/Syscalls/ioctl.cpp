/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Userspace.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$ioctl(int fd, unsigned request, FlatPtr arg)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto description = TRY(fds().open_file_description(fd));
    if (request == FIONBIO) {
        int non_blocking;
        TRY(copy_from_user(&non_blocking, Userspace<const int*>(arg)));
        description->set_blocking(non_blocking == 0);
        return 0;
    }
    TRY(description->file().ioctl(*description, request, arg));
    return 0;
}

}
