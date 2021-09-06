/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/DevFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>

namespace Kernel {

KResultOr<NonnullRefPtr<DevFS>> DevFS::try_create()
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) DevFS);
}

DevFS::DevFS()
{
}

void DevFS::notify_new_device(Device& device)
{
    // FIXME: Handle KString allocation failure.
    auto name = KString::try_create(device.device_name()).release_value();
    MutexLocker locker(m_lock);
    auto new_device_inode = adopt_ref(*new DevFSDeviceInode(*this, device, move(name)));
    m_nodes.append(new_device_inode);
    m_root_inode->m_devices.append(new_device_inode);
}

size_t DevFS::allocate_inode_index()
{
    MutexLocker locker(m_lock);
    m_next_inode_index = m_next_inode_index.value() + 1;
    VERIFY(m_next_inode_index > 0);
    return 1 + m_next_inode_index.value();
}

void DevFS::notify_device_removal(Device&)
{
    TODO();
}

DevFS::~DevFS()
{
}

KResult DevFS::initialize()
{
    m_root_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevFSRootDirectoryInode(*this)));
    Device::for_each([&](Device& device) {
        // FIXME: Find a better way to not add MasterPTYs or SlavePTYs!
        if (device.is_master_pty() || (device.is_character_device() && device.major() == 201))
            return;
        notify_new_device(device);
    });
    return KSuccess;
}

Inode& DevFS::root_inode()
{
    return *m_root_inode;
}

KResultOr<NonnullRefPtr<Inode>> DevFS::get_inode(InodeIdentifier inode_id) const
{
    MutexLocker locker(m_lock);
    if (inode_id.index() == 1)
        return *m_root_inode;
    for (auto& node : m_nodes) {
        if (inode_id.index() == node.index())
            return node;
    }
    return ENOENT;
}

DevFSInode::DevFSInode(DevFS& fs)
    : Inode(fs, fs.allocate_inode_index())
{
}

KResultOr<size_t> DevFSInode::read_bytes(off_t, size_t, UserOrKernelBuffer&, FileDescription*) const
{
    VERIFY_NOT_REACHED();
}

KResult DevFSInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

KResultOr<NonnullRefPtr<Inode>> DevFSInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

void DevFSInode::flush_metadata()
{
}

KResultOr<size_t> DevFSInode::write_bytes(off_t, size_t, const UserOrKernelBuffer&, FileDescription*)
{
    VERIFY_NOT_REACHED();
}

KResultOr<NonnullRefPtr<Inode>> DevFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

KResult DevFSInode::add_child(Inode&, const StringView&, mode_t)
{
    return EROFS;
}

KResult DevFSInode::remove_child(const StringView&)
{
    return EROFS;
}

KResult DevFSInode::chmod(mode_t)
{
    return EPERM;
}

KResult DevFSInode::chown(UserID, GroupID)
{
    return EPERM;
}

KResult DevFSInode::truncate(u64)
{
    return EPERM;
}

StringView DevFSLinkInode::name() const
{
    return m_name->view();
}

DevFSLinkInode::~DevFSLinkInode()
{
}

DevFSLinkInode::DevFSLinkInode(DevFS& fs, NonnullOwnPtr<KString> name)
    : DevFSInode(fs)
    , m_name(move(name))
{
}

KResultOr<size_t> DevFSLinkInode::read_bytes(off_t offset, size_t, UserOrKernelBuffer& buffer, FileDescription*) const
{
    MutexLocker locker(m_inode_lock);
    VERIFY(offset == 0);
    VERIFY(m_link);
    if (!buffer.write(m_link->characters() + offset, m_link->length()))
        return EFAULT;
    return m_link->length();
}

InodeMetadata DevFSLinkInode::metadata() const
{
    InodeMetadata metadata;
    metadata.inode = { fsid(), index() };
    metadata.mode = S_IFLNK | 0555;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}

KResultOr<size_t> DevFSLinkInode::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& buffer, FileDescription*)
{
    auto new_string = TRY(buffer.try_copy_into_kstring(count));

    MutexLocker locker(m_inode_lock);
    VERIFY(offset == 0);
    VERIFY(buffer.is_kernel_buffer());
    m_link = move(new_string);
    return count;
}

DevFSDirectoryInode::DevFSDirectoryInode(DevFS& fs)
    : DevFSInode(fs)
{
}
DevFSDirectoryInode::~DevFSDirectoryInode()
{
}

InodeMetadata DevFSDirectoryInode::metadata() const
{
    InodeMetadata metadata;
    metadata.inode = { fsid(), 1 };
    metadata.mode = 0040555;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}

DevFSRootDirectoryInode::DevFSRootDirectoryInode(DevFS& fs)
    : DevFSDirectoryInode(fs)
{
}

KResult DevFSRootDirectoryInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(fs().m_lock);
    callback({ ".", identifier(), 0 });
    callback({ "..", identifier(), 0 });

    for (auto& directory : m_subdirectories) {
        InodeIdentifier identifier = { fsid(), directory.index() };
        callback({ directory.name(), identifier, 0 });
    }
    for (auto& link : m_links) {
        InodeIdentifier identifier = { fsid(), link.index() };
        callback({ link.name(), identifier, 0 });
    }

    for (auto& device_node : m_devices) {
        InodeIdentifier identifier = { fsid(), device_node.index() };
        callback({ device_node.name(), identifier, 0 });
    }
    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> DevFSRootDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(fs().m_lock);
    for (auto& subdirectory : m_subdirectories) {
        if (subdirectory.name() == name)
            return subdirectory;
    }
    for (auto& link : m_links) {
        if (link.name() == name)
            return link;
    }

    for (auto& device_node : m_devices) {
        if (device_node.name() == name) {
            return device_node;
        }
    }
    return ENOENT;
}
KResultOr<NonnullRefPtr<Inode>> DevFSRootDirectoryInode::create_child(StringView name, mode_t mode, dev_t, UserID, GroupID)
{
    MutexLocker locker(fs().m_lock);

    InodeMetadata metadata;
    metadata.mode = mode;
    if (metadata.is_directory()) {
        for (auto& directory : m_subdirectories) {
            if (directory.name() == name)
                return EEXIST;
        }
        if (name != "pts")
            return EROFS;
        auto new_directory_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevFSPtsDirectoryInode(fs())));
        if (!m_subdirectories.try_ensure_capacity(m_subdirectories.size() + 1))
            return ENOMEM;
        if (!fs().m_nodes.try_ensure_capacity(fs().m_nodes.size() + 1))
            return ENOMEM;
        m_subdirectories.append(*new_directory_inode);
        fs().m_nodes.append(*new_directory_inode);
        return KResult(KSuccess);
    }
    if (metadata.is_symlink()) {
        for (auto& link : m_links) {
            if (link.name() == name)
                return EEXIST;
        }
        auto name_kstring = TRY(KString::try_create(name));
        auto new_link_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevFSLinkInode(fs(), move(name_kstring))));
        if (!m_links.try_ensure_capacity(m_links.size() + 1))
            return ENOMEM;
        if (!fs().m_nodes.try_ensure_capacity(fs().m_nodes.size() + 1))
            return ENOMEM;
        m_links.append(*new_link_inode);
        fs().m_nodes.append(*new_link_inode);
        return new_link_inode;
    }
    return EROFS;
}

DevFSRootDirectoryInode::~DevFSRootDirectoryInode()
{
}
InodeMetadata DevFSRootDirectoryInode::metadata() const
{
    InodeMetadata metadata;
    metadata.inode = { fsid(), 1 };
    metadata.mode = 0040555;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}

DevFSDeviceInode::DevFSDeviceInode(DevFS& fs, Device const& device, NonnullOwnPtr<KString> name)
    : DevFSInode(fs)
    , m_attached_device(device)
    , m_name(move(name))
{
}

DevFSDeviceInode::~DevFSDeviceInode()
{
}

KResult DevFSDeviceInode::chown(UserID uid, GroupID gid)
{
    MutexLocker locker(m_inode_lock);
    m_uid = uid;
    m_gid = gid;
    return KSuccess;
}

StringView DevFSDeviceInode::name() const
{
    return m_name->view();
}

KResultOr<size_t> DevFSDeviceInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* description) const
{
    MutexLocker locker(m_inode_lock);
    VERIFY(!!description);
    if (!m_attached_device->can_read(*description, offset))
        return 0;
    auto nread = const_cast<Device&>(*m_attached_device).read(*description, offset, buffer, count);
    if (nread.is_error())
        return EIO;
    return nread.value();
}

InodeMetadata DevFSDeviceInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), index() };
    metadata.mode = (m_attached_device->is_block_device() ? S_IFBLK : S_IFCHR) | m_attached_device->required_mode();
    metadata.uid = m_uid;
    metadata.gid = m_gid;
    metadata.size = 0;
    metadata.mtime = mepoch;
    metadata.major_device = m_attached_device->major();
    metadata.minor_device = m_attached_device->minor();
    return metadata;
}
KResultOr<size_t> DevFSDeviceInode::write_bytes(off_t offset, size_t count, const UserOrKernelBuffer& buffer, FileDescription* description)
{
    MutexLocker locker(m_inode_lock);
    VERIFY(!!description);
    if (!m_attached_device->can_write(*description, offset))
        return 0;
    auto nread = const_cast<Device&>(*m_attached_device).write(*description, offset, buffer, count);
    if (nread.is_error())
        return EIO;
    return nread.value();
}

DevFSPtsDirectoryInode::DevFSPtsDirectoryInode(DevFS& fs)
    : DevFSDirectoryInode(fs)
{
}
KResult DevFSPtsDirectoryInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(m_inode_lock);
    callback({ ".", identifier(), 0 });
    callback({ "..", identifier(), 0 });
    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> DevFSPtsDirectoryInode::lookup(StringView)
{
    return ENOENT;
}

DevFSPtsDirectoryInode::~DevFSPtsDirectoryInode()
{
}
InodeMetadata DevFSPtsDirectoryInode::metadata() const
{
    InodeMetadata metadata;
    metadata.inode = { fsid(), index() };
    metadata.mode = 0040555;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}

}
