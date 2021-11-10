/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/TTY/SlavePTY.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<DevPtsFS>> DevPtsFS::try_create()
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) DevPtsFS);
}

DevPtsFS::DevPtsFS()
{
}

DevPtsFS::~DevPtsFS()
{
}

ErrorOr<void> DevPtsFS::initialize()
{
    m_root_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevPtsFSInode(*this, 1, nullptr)));
    m_root_inode->m_metadata.inode = { fsid(), 1 };
    m_root_inode->m_metadata.mode = 0040555;
    m_root_inode->m_metadata.uid = 0;
    m_root_inode->m_metadata.gid = 0;
    m_root_inode->m_metadata.size = 0;
    m_root_inode->m_metadata.mtime = mepoch;
    return {};
}

static unsigned inode_index_to_pty_index(InodeIndex inode_index)
{
    VERIFY(inode_index > 1);
    return inode_index.value() - 2;
}

static InodeIndex pty_index_to_inode_index(unsigned pty_index)
{
    return pty_index + 2;
}

Inode& DevPtsFS::root_inode()
{
    return *m_root_inode;
}

ErrorOr<NonnullRefPtr<Inode>> DevPtsFS::get_inode(InodeIdentifier inode_id) const
{
    if (inode_id.index() == 1)
        return *m_root_inode;

    unsigned pty_index = inode_index_to_pty_index(inode_id.index());
    auto* device = DeviceManagement::the().get_device(201, pty_index);
    VERIFY(device);

    auto inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevPtsFSInode(const_cast<DevPtsFS&>(*this), inode_id.index(), static_cast<SlavePTY*>(device))));
    inode->m_metadata.inode = inode_id;
    inode->m_metadata.size = 0;
    inode->m_metadata.uid = device->uid();
    inode->m_metadata.gid = device->gid();
    inode->m_metadata.mode = 0020600;
    inode->m_metadata.major_device = device->major();
    inode->m_metadata.minor_device = device->minor();
    inode->m_metadata.mtime = mepoch;
    return inode;
}

DevPtsFSInode::DevPtsFSInode(DevPtsFS& fs, InodeIndex index, SlavePTY* pty)
    : Inode(fs, index)
{
    if (pty)
        m_pty = *pty;
}

DevPtsFSInode::~DevPtsFSInode()
{
}

ErrorOr<size_t> DevPtsFSInode::read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<size_t> DevPtsFSInode::write_bytes(off_t, size_t, const UserOrKernelBuffer&, OpenFileDescription*)
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

    TRY(callback({ ".", identifier(), 0 }));
    TRY(callback({ "..", identifier(), 0 }));

    return SlavePTY::all_instances().with([&](auto& list) -> ErrorOr<void> {
        for (SlavePTY& slave_pty : list) {
            StringBuilder builder;
            builder.appendff("{}", slave_pty.index());
            TRY(callback({ builder.string_view(), { fsid(), pty_index_to_inode_index(slave_pty.index()) }, 0 }));
        }
        return {};
    });
}

ErrorOr<NonnullRefPtr<Inode>> DevPtsFSInode::lookup(StringView name)
{
    VERIFY(identifier().index() == 1);

    if (name == "." || name == "..")
        return *this;

    auto pty_index = name.to_uint();
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
