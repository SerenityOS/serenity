/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$ioctl(int fd, unsigned request, FlatPtr arg)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto description = TRY(fds().file_description(fd));
    if (request == FIONBIO) {
        description->set_blocking(arg == 0);
        return KSuccess;
    }
    return description->file().ioctl(*description, request, arg);
}

}
