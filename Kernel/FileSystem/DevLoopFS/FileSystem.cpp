/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/DeviceFileTypes.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Loop/LoopDevice.h>
#include <Kernel/FileSystem/DevLoopFS/FileSystem.h>
#include <Kernel/FileSystem/DevLoopFS/Inode.h>
#include <Kernel/FileSystem/RAMBackedFileType.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FileSystem>> DevLoopFS::try_create(FileSystemSpecificOptions const&)
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevLoopFS));
}

DevLoopFS::DevLoopFS() = default;
DevLoopFS::~DevLoopFS() = default;

u8 DevLoopFS::internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const
{
    return ram_backed_file_type_to_directory_entry_type(entry);
}

ErrorOr<void> DevLoopFS::initialize()
{
    m_root_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevLoopFSInode(*this)));
    m_root_inode->m_metadata.inode = { fsid(), 1 };
    m_root_inode->m_metadata.mode = S_IFDIR
        | S_IROTH | S_IRGRP | S_IRUSR
        | S_IXUSR | S_IXGRP | S_IXOTH;
    m_root_inode->m_metadata.uid = 0;
    m_root_inode->m_metadata.gid = 0;
    m_root_inode->m_metadata.size = 0;
    m_root_inode->m_metadata.mtime = TimeManagement::boot_time();
    return {};
}

static unsigned inode_index_to_loop_index(InodeIndex inode_index)
{
    VERIFY(inode_index > 1);
    return inode_index.value() - 2;
}

Inode& DevLoopFS::root_inode()
{
    return *m_root_inode;
}

ErrorOr<void> DevLoopFS::rename(Inode&, StringView, Inode&, StringView)
{
    return EROFS;
}

ErrorOr<NonnullRefPtr<Inode>> DevLoopFS::get_inode(InodeIdentifier inode_id) const
{
    if (inode_id.index() == 1)
        return *m_root_inode;

    unsigned loop_index = inode_index_to_loop_index(inode_id.index());
    auto device = Device::acquire_by_type_and_major_minor_numbers(DeviceNodeType::Block, 20, loop_index);
    VERIFY(device);

    auto& loop_device = static_cast<LoopDevice&>(*device);
    auto inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevLoopFSInode(const_cast<DevLoopFS&>(*this), inode_id.index(), loop_device)));
    inode->m_metadata.inode = inode_id;
    inode->m_metadata.size = 0;
    inode->m_metadata.uid = 0;
    inode->m_metadata.gid = 0;
    inode->m_metadata.mode = S_IFBLK | S_IRUSR | S_IWUSR;
    inode->m_metadata.major_device = device->major();
    inode->m_metadata.minor_device = device->minor();
    inode->m_metadata.mtime = TimeManagement::boot_time();
    return inode;
}

}
