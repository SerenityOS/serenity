/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$dup2(int old_fd, int new_fd)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
        auto description = TRY(fds.open_file_description(old_fd));
        if (old_fd == new_fd)
            return new_fd;
        if (new_fd < 0 || static_cast<size_t>(new_fd) >= OpenFileDescriptions::max_open())
            return EINVAL;
        if (!fds.m_fds_metadatas[new_fd].is_allocated())
            fds.m_fds_metadatas[new_fd].allocate();
        fds[new_fd].set(move(description));
        return new_fd;
    });
}

}
