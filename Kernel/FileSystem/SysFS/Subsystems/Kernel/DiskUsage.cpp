/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/API/POSIX/unistd.h>
#include <Kernel/Devices/Loop/LoopDevice.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/DiskUsage.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSDiskUsage> SysFSDiskUsage::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSDiskUsage(parent_directory)).release_nonnull();
}

UNMAP_AFTER_INIT SysFSDiskUsage::SysFSDiskUsage(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

ErrorOr<void> SysFSDiskUsage::try_generate(KBufferBuilder& builder)
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(Process::current().vfs_root_context()->for_each_mount([&array](auto& mount) -> ErrorOr<void> {
        auto& fs = mount.guest_fs();
        auto fs_object = TRY(array.add_object());
        TRY(fs_object.add("class_name"sv, fs.class_name()));
        TRY(fs_object.add("total_block_count"sv, fs.total_block_count()));
        TRY(fs_object.add("free_block_count"sv, fs.free_block_count()));
        TRY(fs_object.add("total_inode_count"sv, fs.total_inode_count()));
        TRY(fs_object.add("free_inode_count"sv, fs.free_inode_count()));
        auto mount_point = TRY(mount.absolute_path());
        TRY(fs_object.add("mount_point"sv, mount_point->view()));
        TRY(fs_object.add("block_size"sv, static_cast<u64>(fs.logical_block_size())));
        TRY(fs_object.add("readonly"sv, fs.is_readonly()));
        TRY(fs_object.add("mount_flags"sv, mount.flags()));

        if (mount.flags() & MS_SRCHIDDEN) {
            TRY(fs_object.add("source"sv, "unknown"));
        } else {
            if (fs.is_file_backed()) {
                auto& file = static_cast<FileBackedFileSystem const&>(fs).file();
                if (file.is_loop_device()) {
                    auto& device = static_cast<LoopDevice const&>(file);
                    auto path = TRY(device.custody().try_serialize_absolute_path());
                    TRY(fs_object.add("source"sv, path->view()));
                } else {
                    auto pseudo_path = TRY(static_cast<FileBackedFileSystem const&>(fs).file_description().pseudo_path());
                    TRY(fs_object.add("source"sv, pseudo_path->view()));
                }
            } else {
                TRY(fs_object.add("source"sv, "none"));
            }
        }

        TRY(fs_object.finish());
        return {};
    }));
    TRY(array.finish());
    return {};
}

}
