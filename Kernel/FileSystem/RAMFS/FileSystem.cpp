/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/FileSystem/RAMFS/Inode.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FileSystem>> RAMFS::try_create()
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RAMFS));
}

RAMFS::RAMFS() = default;
RAMFS::~RAMFS() = default;

ErrorOr<void> RAMFS::initialize()
{
    m_root_inode = TRY(RAMFSInode::try_create_root(*this));
    return {};
}

Inode& RAMFS::root_inode()
{
    VERIFY(!m_root_inode.is_null());
    return *m_root_inode;
}

unsigned RAMFS::next_inode_index()
{
    MutexLocker locker(m_lock);

    return m_next_inode_index++;
}

}
