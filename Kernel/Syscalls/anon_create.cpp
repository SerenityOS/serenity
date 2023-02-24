/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/AnonymousFile.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$anon_create(size_t size, int options)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    if (!size)
        return EINVAL;

    if (size % PAGE_SIZE)
        return EINVAL;

    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;

    auto vmobject = TRY(Memory::AnonymousVMObject::try_create_purgeable_with_size(size, AllocationStrategy::AllocateNow));
    auto anon_file = TRY(AnonymousFile::try_create(move(vmobject)));
    auto description = TRY(OpenFileDescription::try_create(move(anon_file)));

    description->set_writable(true);
    description->set_readable(true);

    u32 fd_flags = 0;
    if (options & O_CLOEXEC)
        fd_flags |= FD_CLOEXEC;

    return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
        auto new_fd = TRY(fds.allocate());
        fds[new_fd.fd].set(description, fd_flags);
        return new_fd.fd;
    });
}

}
