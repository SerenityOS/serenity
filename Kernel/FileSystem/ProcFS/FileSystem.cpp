/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/DirectoryInode.h>
#include <Kernel/FileSystem/ProcFS/FileSystem.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<FileSystem>> ProcFS::try_create()
{
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFS));
}

ProcFS::ProcFS() = default;
ProcFS::~ProcFS() = default;

ErrorOr<void> ProcFS::initialize()
{
    m_root_inode = static_ptr_cast<ProcFSDirectoryInode>(TRY(ProcFSComponentRegistry::the().root_directory().to_inode(*this)));
    return {};
}

Inode& ProcFS::root_inode()
{
    return *m_root_inode;
}

}
