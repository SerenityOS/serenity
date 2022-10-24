/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/Inode.h>

namespace Kernel {

ProcFSInode::ProcFSInode(ProcFS const& fs, InodeIndex index)
    : Inode(const_cast<ProcFS&>(fs), index)
{
}

ProcFSInode::~ProcFSInode() = default;

ErrorOr<void> ProcFSInode::flush_metadata()
{
    return {};
}

ErrorOr<void> ProcFSInode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> ProcFSInode::remove_child(StringView)
{
    return EROFS;
}

ErrorOr<void> ProcFSInode::chmod(mode_t)
{
    return EPERM;
}

ErrorOr<void> ProcFSInode::chown(UserID, GroupID)
{
    return EPERM;
}

}
