/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/RAMBackedFileType.h>
#include <Kernel/FileSystem/SysFS/FileSystem.h>
#include <Kernel/FileSystem/SysFS/Inode.h>
#include <Kernel/FileSystem/SysFS/Registry.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FileSystem>> SysFS::try_create(FileSystemSpecificOptions const&)
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SysFS));
}

SysFS::SysFS() = default;
SysFS::~SysFS() = default;

ErrorOr<void> SysFS::initialize()
{
    m_root_inode = TRY(SysFSComponentRegistry::the().root_directory().to_inode(*this));
    return {};
}

u8 SysFS::internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const
{
    return ram_backed_file_type_to_directory_entry_type(entry);
}

Inode& SysFS::root_inode()
{
    return *m_root_inode;
}

ErrorOr<void> SysFS::rename(Inode&, StringView, Inode&, StringView)
{
    return EROFS;
}

}
