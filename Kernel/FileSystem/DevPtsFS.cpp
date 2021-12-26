/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/TTY/SlavePTY.h>

namespace Kernel {

NonnullRefPtr<DevPtsFS> DevPtsFS::create()
{
    return adopt_ref(*new DevPtsFS);
}

DevPtsFS::DevPtsFS()
{
}

DevPtsFS::~DevPtsFS()
{
}

KResult DevPtsFS::initialize()
{
    m_root_inode = adopt_ref_if_nonnull(new (nothrow) DevPtsFSInode(*this, 1, nullptr));
    if (!m_root_inode)
        return ENOMEM;

    m_root_inode->m_metadata.inode = { fsid(), 1 };
    m_root_inode->m_metadata.mode = 0040555;
    m_root_inode->m_metadata.uid = 0;
    m_root_inode->m_metadata.gid = 0;
    m_root_inode->m_metadata.size = 0;
    m_root_inode->m_metadata.mtime = mepoch;
    return KSuccess;
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

KResultOr<NonnullRefPtr<Inode>> DevPtsFS::get_inode(InodeIdentifier inode_id) const
{
    if (inode_id.index() == 1)
        return *m_root_inode;

    unsigned pty_index = inode_index_to_pty_index(inode_id.index());
    auto* device = Device::get_device(201, pty_index);
    VERIFY(device);

    // FIXME: Handle OOM
    auto inode = adopt_ref(*new DevPtsFSInode(const_cast<DevPtsFS&>(*this), inode_id.index(), static_cast<SlavePTY*>(device)));
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

KResultOr<size_t> DevPtsFSInode::read_bytes(off_t, size_t, UserOrKernelBuffer&, FileDescription*) const
{
    VERIFY_NOT_REACHED();
}

KResultOr<size_t> DevPtsFSInode::write_bytes(off_t, size_t, const UserOrKernelBuffer&, FileDescription*)
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

KResult DevPtsFSInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    if (identifier().index() > 1)
        return ENOTDIR;

    callback({ ".", identifier(), 0 });
    callback({ "..", identifier(), 0 });

    SlavePTY::all_instances().with([&](auto& list) {
        for (SlavePTY& slave_pty : list) {
            StringBuilder builder;
            builder.appendff("{}", slave_pty.index());
            callback({ builder.string_view(), { fsid(), pty_index_to_inode_index(slave_pty.index()) }, 0 });
        }
    });

    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> DevPtsFSInode::lookup(StringView name)
{
    VERIFY(identifier().index() == 1);

    if (name == "." || name == "..")
        return *this;

    auto pty_index = name.to_uint();
    if (!pty_index.has_value())
        return ENOENT;

    return SlavePTY::all_instances().with([&](auto& list) -> KResultOr<NonnullRefPtr<Inode>> {
        for (SlavePTY& slave_pty : list) {
            if (slave_pty.index() != pty_index.value())
                continue;
            return fs().get_inode({ fsid(), pty_index_to_inode_index(pty_index.value()) });
        }
        return ENOENT;
    });
}

void DevPtsFSInode::flush_metadata()
{
}

KResult DevPtsFSInode::add_child(Inode&, const StringView&, mode_t)
{
    return EROFS;
}

KResultOr<NonnullRefPtr<Inode>> DevPtsFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

KResult DevPtsFSInode::remove_child(const StringView&)
{
    return EROFS;
}

KResult DevPtsFSInode::chmod(mode_t)
{
    return EROFS;
}

KResult DevPtsFSInode::chown(UserID, GroupID)
{
    return EROFS;
}

}
