/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/CoredumpFile.h>
#include <Kernel/FileSystem/CoredumpFS/Inode.h>
#include <Kernel/Process.h>

namespace Kernel {

static unsigned inode_index_to_coredump_pid_index(InodeIndex inode_index)
{
    VERIFY(inode_index > 1);
    return inode_index.value() - 2;
}

CoredumpFSInode::CoredumpFSInode(CoredumpFS& fs, InodeIndex index)
    : Inode(fs, index)
    , m_associated_coredump_pid(index.value() > 1 ? inode_index_to_coredump_pid_index(index) : Optional<CoredumpPID> {})
{
}

CoredumpFSInode::~CoredumpFSInode() = default;

ErrorOr<size_t> CoredumpFSInode::read_bytes_locked(off_t offset, size_t length, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto coredump = CoredumpFile::from_pid_in_same_associated_jail(inode_index_to_coredump_pid_index(identifier().index()));
    if (!coredump)
        return EIO;
    return coredump->read({}, offset, buffer, length);
}

ErrorOr<void> CoredumpFSInode::truncate(u64 size)
{
    auto coredump = CoredumpFile::from_pid_in_same_associated_jail(inode_index_to_coredump_pid_index(identifier().index()));
    if (!coredump)
        return EIO;
    if (size == 0)
        coredump->truncate({});
    return {};
}

ErrorOr<size_t> CoredumpFSInode::write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    return EROFS;
}

InodeMetadata CoredumpFSInode::metadata() const
{
    if (identifier().index() == 1) {
        InodeMetadata metadata;
        metadata.inode = { fsid(), 1 };
        metadata.mode = 0040777;
        metadata.uid = 0;
        metadata.gid = 0;
        metadata.size = 0;
        metadata.mtime = TimeManagement::boot_time();
        return metadata;
    }
    auto coredump = CoredumpFile::from_pid_in_same_associated_jail(inode_index_to_coredump_pid_index(identifier().index()));
    if (!coredump)
        return {};
    InodeMetadata metadata;
    metadata.inode = { fsid(), 1 };
    metadata.mode = S_IFREG | 0600;
    metadata.uid = coredump->associated_uid();
    metadata.gid = coredump->associated_gid();
    metadata.size = coredump->size();
    metadata.mtime = coredump->creation_time();
    return metadata;
}

ErrorOr<void> CoredumpFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    if (identifier().index() > 1)
        return ENOTDIR;

    TRY(callback({ "."sv, identifier(), 0 }));
    TRY(callback({ ".."sv, identifier(), 0 }));

    return CoredumpFile::for_each_in_same_associated_jail([&](CoredumpFile& coredump_file) -> ErrorOr<void> {
        StringBuilder builder;
        TRY(builder.try_appendff("{}", coredump_file.associated_pid()));
        TRY(callback({ builder.string_view(), { fsid(), CoredumpFS::coredump_pid_index_to_inode_index(coredump_file.associated_pid()) }, 0 }));
        return {};
    });
}

ErrorOr<NonnullLockRefPtr<Inode>> CoredumpFSInode::lookup(StringView name)
{
    VERIFY(identifier().index() == 1);

    if (name == "." || name == "..")
        return *this;

    auto coredump_index = name.to_uint();
    if (!coredump_index.has_value())
        return ENOENT;

    auto coredump_file = CoredumpFile::from_pid_in_same_associated_jail(coredump_index.value());
    if (coredump_file) {
        return fs().get_inode({ fsid(), CoredumpFS::coredump_pid_index_to_inode_index(coredump_index.value()) });
    }
    return ENOENT;
}

ErrorOr<void> CoredumpFSInode::flush_metadata()
{
    return {};
}

ErrorOr<void> CoredumpFSInode::add_child(Inode&, StringView, mode_t)
{
    return EPERM;
}

ErrorOr<NonnullLockRefPtr<Inode>> CoredumpFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EPERM;
}

ErrorOr<void> CoredumpFSInode::remove_child(StringView path)
{
    VERIFY(identifier().index() == 1);
    VERIFY(!(path == "." || path == ".."));
    auto coredump_index = path.to_uint();
    if (!coredump_index.has_value())
        return ENOENT;

    TRY(CoredumpFile::all_instances().with([&](auto&) -> ErrorOr<void> {
        auto coredump_file = CoredumpFile::from_pid_in_same_associated_jail(coredump_index.value());
        if (!coredump_file)
            return ENOENT;
        coredump_file->m_list_node.remove();
        return {};
    }));
    return {};
}

ErrorOr<void> CoredumpFSInode::replace_child(StringView, Inode&)
{
    VERIFY(identifier().index() == 1);
    return EPERM;
}

ErrorOr<void> CoredumpFSInode::chmod(mode_t)
{
    return EPERM;
}

ErrorOr<void> CoredumpFSInode::chown(UserID, GroupID)
{
    return EPERM;
}

}
