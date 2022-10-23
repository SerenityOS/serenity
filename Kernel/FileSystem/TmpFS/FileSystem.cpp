/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/TmpFS/FileSystem.h>
#include <Kernel/FileSystem/TmpFS/Inode.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<FileSystem>> TmpFS::try_create()
{
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) TmpFS));
}

TmpFS::TmpFS() = default;
TmpFS::~TmpFS() = default;

ErrorOr<void> TmpFS::initialize()
{
    m_root_inode = TRY(TmpFSInode::try_create_root(*this));
    return {};
}

Inode& TmpFS::root_inode()
{
    VERIFY(!m_root_inode.is_null());
    return *m_root_inode;
}

unsigned TmpFS::next_inode_index()
{
    MutexLocker locker(m_lock);

    return m_next_inode_index++;
}

}
