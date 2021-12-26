/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/AnonymousFile.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$anon_create(size_t size, int options)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(stdio);

    if (!size)
        return EINVAL;

    if (size % PAGE_SIZE)
        return EINVAL;

    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;

    auto new_fd_or_error = m_fds.allocate();
    if (new_fd_or_error.is_error())
        return new_fd_or_error.error();
    auto new_fd = new_fd_or_error.release_value();
    auto vmobject = Memory::AnonymousVMObject::try_create_purgeable_with_size(size, AllocationStrategy::Reserve);
    if (!vmobject)
        return ENOMEM;

    auto anon_file = AnonymousFile::create(vmobject.release_nonnull());
    if (!anon_file)
        return ENOMEM;
    auto description_or_error = FileDescription::create(*anon_file);
    if (description_or_error.is_error())
        return description_or_error.error();

    auto description = description_or_error.release_value();
    description->set_writable(true);
    description->set_readable(true);

    u32 fd_flags = 0;
    if (options & O_CLOEXEC)
        fd_flags |= FD_CLOEXEC;

    m_fds[new_fd.fd].set(move(description), fd_flags);
    return new_fd.fd;
}

}
