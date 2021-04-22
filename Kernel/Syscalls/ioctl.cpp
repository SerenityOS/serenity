/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

KResultOr<int> Process::sys$ioctl(int fd, unsigned request, FlatPtr arg)
{
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    if (request == FIONBIO) {
        description->set_blocking(arg == 0);
        return KSuccess;
    }
    return description->file().ioctl(*description, request, arg);
}

}
