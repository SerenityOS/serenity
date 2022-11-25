/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/DevPtsFS/Inode.h>

namespace Kernel {

static InodeIndex pty_index_to_inode_index(unsigned pty_index)
{
    return pty_index + 2;
}

DevPtsFSInode::DevPtsFSInode(DevPtsFS& fs, InodeIndex index, SlavePTY* pty)
    : Inode(fs, index)
{
    if (pty)
        m_pty = *pty;
}

DevPtsFSInode::~DevPtsFSInode() = default;

ErrorOr<size_t> DevPtsFSInode::read_bytes_locked(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<size_t> DevPtsFSInode::write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata DevPtsFSInode::metadata() const
{
    if (auto pty = m_pty.strong_ref()) {
        auto metadata = m_metadata;
        metadata.mtime = Time::from_timespec({ pty->time_of_last_write(), 0 });
        return metadata;
    }
    return m_metadata;
}

ErrorOr<void> DevPtsFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    if (identifier().index() > 1)
        return ENOTDIR;

    TRY(callback({ "."sv, identifier(), 0 }));
    TRY(callback({ ".."sv, identifier(), 0 }));

    return SlavePTY::all_instances().with([&](auto& list) -> ErrorOr<void> {
        StringBuilder builder;
        for (SlavePTY& slave_pty : list) {
            builder.clear();
            TRY(builder.try_appendff("{}", slave_pty.index()));
            TRY(callback({ builder.string_view(), { fsid(), pty_index_to_inode_index(slave_pty.index()) }, 0 }));
        }
        return {};
    });
}

ErrorOr<NonnullLockRefPtr<Inode>> DevPtsFSInode::lookup(StringView name)
{
    VERIFY(identifier().index() == 1);

    if (name == "." || name == "..")
        return *this;

    auto pty_index = name.to_uint();
    if (!pty_index.has_value())
        return ENOENT;

    return SlavePTY::all_instances().with([&](auto& list) -> ErrorOr<NonnullLockRefPtr<Inode>> {
        for (SlavePTY& slave_pty : list) {
            if (slave_pty.index() != pty_index.value())
                continue;
            return fs().get_inode({ fsid(), pty_index_to_inode_index(pty_index.value()) });
        }
        return ENOENT;
    });
}

ErrorOr<void> DevPtsFSInode::flush_metadata()
{
    return {};
}

ErrorOr<void> DevPtsFSInode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<NonnullLockRefPtr<Inode>> DevPtsFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> DevPtsFSInode::remove_child(StringView)
{
    return EROFS;
}

ErrorOr<void> DevPtsFSInode::replace_child(StringView, Inode&)
{
    return EROFS;
}

ErrorOr<void> DevPtsFSInode::chmod(mode_t)
{
    return EROFS;
}

ErrorOr<void> DevPtsFSInode::chown(UserID, GroupID)
{
    return EROFS;
}

}
