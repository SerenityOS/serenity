/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$dup2(int old_fd, int new_fd)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(stdio);
    auto description = TRY(fds().file_description(old_fd));
    if (old_fd == new_fd)
        return new_fd;
    if (new_fd < 0 || static_cast<size_t>(new_fd) >= fds().max_open())
        return EINVAL;
    if (!m_fds.m_fds_metadatas[new_fd].is_allocated())
        m_fds.m_fds_metadatas[new_fd].allocate();
    m_fds[new_fd].set(move(description));
    return new_fd;
}

}
