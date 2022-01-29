/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/AnonymousFile.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$anon_create(size_t size, int options)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::stdio));

    if (!size)
        return EINVAL;

    if (size % PAGE_SIZE)
        return EINVAL;

    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;

    auto new_fd = TRY(allocate_fd());
    auto vmobject = TRY(Memory::AnonymousVMObject::try_create_purgeable_with_size(size, AllocationStrategy::Reserve));
    auto anon_file = TRY(AnonymousFile::try_create(move(vmobject)));
    auto description = TRY(OpenFileDescription::try_create(move(anon_file)));

    description->set_writable(true);
    description->set_readable(true);

    u32 fd_flags = 0;
    if (options & O_CLOEXEC)
        fd_flags |= FD_CLOEXEC;

    m_fds.with_exclusive([&](auto& fds) { fds[new_fd.fd].set(move(description), fd_flags); });
    return new_fd.fd;
}

}
