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
    auto description = TRY(open_file_description(fd));
    if (request == FIONBIO) {
        description->set_blocking(TRY(copy_typed_from_user(Userspace<int const*>(arg))) == 0);
        return 0;
    }
    TRY(description->file().ioctl(*description, request, arg));
    return 0;
}

}
