/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/DevPtsFS/Inode.h>
#include <Kernel/FileSystem/RAMBackedFileType.h>

namespace Kernel {

static InodeIndex pty_index_to_inode_index(unsigned pty_index)
{
    return pty_index + 2;
}

// NOTE: This constructor is used for the root inode only.
DevPtsFSInode::DevPtsFSInode(DevPtsFS& fs)
    : Inode(fs, 1)
{
}

DevPtsFSInode::DevPtsFSInode(DevPtsFS& fs, InodeIndex index, SlavePTY& pty)
    : Inode(fs, index)
    , m_pty(pty)
{
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
        metadata.mtime = pty->time_of_last_write();
        return metadata;
    }
    return m_metadata;
}

ErrorOr<void> DevPtsFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    if (identifier().index() > 1)
        return ENOTDIR;

    TRY(callback({ "."sv, identifier(), to_underlying(RAMBackedFileType::Directory) }));
    TRY(callback({ ".."sv, identifier(), to_underlying(RAMBackedFileType::Directory) }));

    return SlavePTY::all_instances().with([&](auto& list) -> ErrorOr<void> {
        StringBuilder builder;
        for (SlavePTY& slave_pty : list) {
            builder.clear();
            TRY(builder.try_appendff("{}", slave_pty.index()));
            // NOTE: We represent directory entries with DT_CHR as all
            // inodes in this filesystem are assumed to be char devices.
            TRY(callback({ builder.string_view(), { fsid(), pty_index_to_inode_index(slave_pty.index()) }, to_underlying(RAMBackedFileType::Character) }));
        }
        return {};
    });
}

ErrorOr<NonnullRefPtr<Inode>> DevPtsFSInode::lookup(StringView name)
{
    VERIFY(identifier().index() == 1);

    if (name == "." || name == "..")
        return *this;

    auto pty_index = name.to_number<unsigned>();
    if (!pty_index.has_value())
        return ENOENT;

    return SlavePTY::all_instances().with([&](auto& list) -> ErrorOr<NonnullRefPtr<Inode>> {
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

ErrorOr<NonnullRefPtr<Inode>> DevPtsFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> DevPtsFSInode::remove_child(StringView)
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
