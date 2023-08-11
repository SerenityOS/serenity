/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$ftruncate(int fd, off_t length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    if (length < 0)
        return EINVAL;
    auto description = TRY(open_file_description(fd));
    if (!description->is_writable())
        return EBADF;
    TRY(description->truncate(static_cast<u64>(length)));
    return 0;
}

}
