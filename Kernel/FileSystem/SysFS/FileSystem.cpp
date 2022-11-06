/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/SysFS/FileSystem.h>
#include <Kernel/FileSystem/SysFS/Inode.h>
#include <Kernel/FileSystem/SysFS/Registry.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<FileSystem>> SysFS::try_create()
{
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) SysFS));
}

SysFS::SysFS() = default;
SysFS::~SysFS() = default;

ErrorOr<void> SysFS::initialize()
{
    m_root_inode = TRY(SysFSComponentRegistry::the().root_directory().to_inode(*this));
    return {};
}

Inode& SysFS::root_inode()
{
    return *m_root_inode;
}

}
