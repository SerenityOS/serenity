/*
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeFile.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$posix_fallocate(int fd, Userspace<off_t const*> userspace_offset, Userspace<off_t const*> userspace_length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    auto offset = TRY(copy_typed_from_user(userspace_offset));
    if (offset < 0)
        return EINVAL;
    auto length = TRY(copy_typed_from_user(userspace_length));
    if (length <= 0)
        return EINVAL;

    Checked<size_t> checked_size { length };
    checked_size += offset;
    // FIXME: Return EFBIG if offset+length > FileSizeMax
    if (checked_size.has_overflow())
        return EFBIG;

    auto description = TRY(open_file_description(fd));
    if (!description->is_writable())
        return EBADF;

    if (description->is_fifo())
        return ESPIPE;

    // [ENODEV] The fd argument does not refer to a regular file.
    if (!description->file().is_regular_file())
        return ENODEV;

    VERIFY(description->file().is_inode());

    auto& file = static_cast<InodeFile&>(description->file());
    if (file.inode().size() >= checked_size.value())
        return 0;

    // Note: truncate essentially calls resize in the inodes implementation
    //       while resize is not a standard member of an inode, so we just call
    //       truncate instead
    TRY(file.inode().truncate(checked_size.value()));

    // FIXME: EINTR: A signal was caught during execution.
    return 0;
}

}
