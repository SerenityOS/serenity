/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Loop/LoopDevice.h>
#include <Kernel/FileSystem/DevLoopFS/Inode.h>
#include <Kernel/FileSystem/RAMBackedFileType.h>

namespace Kernel {

static InodeIndex loop_index_to_inode_index(unsigned loop_index)
{
    return loop_index + 2;
}

DevLoopFSInode::DevLoopFSInode(DevLoopFS& fs, InodeIndex index, LoopDevice& loop_device)
    : Inode(fs, index)
    , m_loop_device(loop_device)
{
}

// NOTE: This constructor is used for the root inode only.
DevLoopFSInode::DevLoopFSInode(DevLoopFS& fs)
    : Inode(fs, 1)
{
}

DevLoopFSInode::~DevLoopFSInode() = default;

ErrorOr<size_t> DevLoopFSInode::read_bytes_locked(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<size_t> DevLoopFSInode::write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata DevLoopFSInode::metadata() const
{
    return m_metadata;
}

ErrorOr<void> DevLoopFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    if (identifier().index() > 1)
        return ENOTDIR;

    TRY(callback({ "."sv, identifier(), to_underlying(RAMBackedFileType::Directory) }));
    TRY(callback({ ".."sv, identifier(), to_underlying(RAMBackedFileType::Directory) }));

    return LoopDevice::all_instances().with([&](auto& list) -> ErrorOr<void> {
        StringBuilder builder;
        for (LoopDevice& loop_device : list) {
            builder.clear();
            TRY(builder.try_appendff("{}", loop_device.index()));
            TRY(callback({ builder.string_view(), { fsid(), loop_index_to_inode_index(loop_device.index()) }, to_underlying(RAMBackedFileType::Block) }));
        }
        return {};
    });
}

ErrorOr<NonnullRefPtr<Inode>> DevLoopFSInode::lookup(StringView name)
{
    VERIFY(identifier().index() == 1);

    if (name == "." || name == "..")
        return *this;

    auto loop_index = name.to_number<unsigned>();
    if (!loop_index.has_value())
        return ENOENT;

    return LoopDevice::all_instances().with([&](auto& list) -> ErrorOr<NonnullRefPtr<Inode>> {
        for (LoopDevice& loop_device : list) {
            if (loop_device.index() != loop_index.value())
                continue;
            return fs().get_inode({ fsid(), loop_index_to_inode_index(loop_index.value()) });
        }
        return ENOENT;
    });
}

ErrorOr<void> DevLoopFSInode::flush_metadata()
{
    return {};
}

ErrorOr<void> DevLoopFSInode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<NonnullRefPtr<Inode>> DevLoopFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> DevLoopFSInode::remove_child(StringView)
{
    return EROFS;
}

ErrorOr<void> DevLoopFSInode::chmod(mode_t)
{
    return EROFS;
}

ErrorOr<void> DevLoopFSInode::chown(UserID, GroupID)
{
    return EROFS;
}

}
