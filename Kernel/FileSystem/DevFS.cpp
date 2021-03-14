/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/DevFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>

namespace Kernel {

NonnullRefPtr<DevFS> DevFS::create()
{
    return adopt(*new DevFS);
}

DevFS::DevFS()
    : m_root_inode(adopt(*new DevFSRootDirectoryInode(*this)))
{
    LOCKER(m_lock);
    Device::for_each([&](Device& device) {
        // FIXME: Find a better way to not add MasterPTYs or SlavePTYs!
        if (device.is_master_pty() || (device.is_character_device() && device.major() == 201))
            return;
        notify_new_device(device);
    });
}

void DevFS::notify_new_device(Device& device)
{
    LOCKER(m_lock);
    auto new_device_inode = adopt(*new DevFSDeviceInode(*this, device));
    m_nodes.append(new_device_inode);
    m_root_inode->m_devices.append(new_device_inode);
}

size_t DevFS::allocate_inode_index()
{
    LOCKER(m_lock);
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

bool DevFS::initialize()
{
    return true;
}

NonnullRefPtr<Inode> DevFS::root_inode() const
{
    return *m_root_inode;
}

RefPtr<Inode> DevFS::get_inode(InodeIdentifier inode_id) const
{
    LOCKER(m_lock);
    if (inode_id.index() == 1)
        return m_root_inode;
    for (auto& node : m_nodes) {
        if (inode_id.index() == node.index())
            return node;
    }
    return nullptr;
}

DevFSInode::DevFSInode(DevFS& fs)
    : Inode(fs, fs.allocate_inode_index())
{
}
ssize_t DevFSInode::read_bytes(off_t, ssize_t, UserOrKernelBuffer&, FileDescription*) const
{
    VERIFY_NOT_REACHED();
}

KResult DevFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)>) const
{
    VERIFY_NOT_REACHED();
}

RefPtr<Inode> DevFSInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

void DevFSInode::flush_metadata()
{
}

ssize_t DevFSInode::write_bytes(off_t, ssize_t, const UserOrKernelBuffer&, FileDescription*)
{
    VERIFY_NOT_REACHED();
}

KResultOr<NonnullRefPtr<Inode>> DevFSInode::create_child(const String&, mode_t, dev_t, uid_t, gid_t)
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

KResultOr<size_t> DevFSInode::directory_entry_count() const
{
    VERIFY_NOT_REACHED();
}

KResult DevFSInode::chmod(mode_t)
{
    return EPERM;
}

KResult DevFSInode::chown(uid_t, gid_t)
{
    return EPERM;
}

KResult DevFSInode::truncate(u64)
{
    return EPERM;
}

String DevFSLinkInode::name() const
{
    return m_name;
}
DevFSLinkInode::~DevFSLinkInode()
{
}
DevFSLinkInode::DevFSLinkInode(DevFS& fs, String name)
    : DevFSInode(fs)
    , m_name(name)
{
}
ssize_t DevFSLinkInode::read_bytes(off_t offset, ssize_t, UserOrKernelBuffer& buffer, FileDescription*) const
{
    LOCKER(m_lock);
    VERIFY(offset == 0);
    VERIFY(!m_link.is_null());
    if (!buffer.write(((const u8*)m_link.substring_view(0).characters_without_null_termination()) + offset, m_link.length()))
        return -EFAULT;
    return m_link.length();
}
InodeMetadata DevFSLinkInode::metadata() const
{
    LOCKER(m_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), index() };
    metadata.mode = S_IFLNK | 0555;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}
ssize_t DevFSLinkInode::write_bytes(off_t offset, ssize_t count, const UserOrKernelBuffer& buffer, FileDescription*)
{
    LOCKER(m_lock);
    VERIFY(offset == 0);
    VERIFY(buffer.is_kernel_buffer());
    m_link = buffer.copy_into_string(count);
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
    LOCKER(m_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), 1 };
    metadata.mode = 0040555;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}
KResult DevFSDirectoryInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)>) const
{
    LOCKER(m_lock);
    return EINVAL;
}
RefPtr<Inode> DevFSDirectoryInode::lookup(StringView)
{
    LOCKER(m_lock);
    return nullptr;
}
KResultOr<size_t> DevFSDirectoryInode::directory_entry_count() const
{
    LOCKER(m_lock);
    return m_devices.size();
}

DevFSRootDirectoryInode::DevFSRootDirectoryInode(DevFS& fs)
    : DevFSDirectoryInode(fs)
    , m_parent_fs(fs)
{
}
KResult DevFSRootDirectoryInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    LOCKER(m_parent_fs.m_lock);
    callback({ ".", identifier(), 0 });
    callback({ "..", identifier(), 0 });

    for (auto& folder : m_subfolders) {
        InodeIdentifier identifier = { fsid(), folder.index() };
        callback({ folder.name(), identifier, 0 });
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
RefPtr<Inode> DevFSRootDirectoryInode::lookup(StringView name)
{
    LOCKER(m_parent_fs.m_lock);
    for (auto& subfolder : m_subfolders) {
        if (subfolder.name() == name)
            return subfolder;
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
    return nullptr;
}
KResultOr<NonnullRefPtr<Inode>> DevFSRootDirectoryInode::create_child(const String& name, mode_t mode, dev_t, uid_t, gid_t)
{
    LOCKER(m_parent_fs.m_lock);

    InodeMetadata metadata;
    metadata.mode = mode;
    if (metadata.is_directory()) {
        for (auto& folder : m_subfolders) {
            if (folder.name() == name)
                return EEXIST;
        }
        if (name != "pts")
            return EROFS;
        auto new_directory_inode = adopt(*new DevFSPtsDirectoryInode(m_parent_fs));
        m_subfolders.append(new_directory_inode);
        m_parent_fs.m_nodes.append(new_directory_inode);
        return KResult(KSuccess);
    }
    if (metadata.is_symlink()) {
        for (auto& link : m_links) {
            if (link.name() == name)
                return EEXIST;
        }
        auto new_link_inode = adopt(*new DevFSLinkInode(m_parent_fs, name));
        m_links.append(new_link_inode);
        m_parent_fs.m_nodes.append(new_link_inode);
        return new_link_inode;
    }
    return EROFS;
}

DevFSRootDirectoryInode::~DevFSRootDirectoryInode()
{
}
InodeMetadata DevFSRootDirectoryInode::metadata() const
{
    LOCKER(m_parent_fs.m_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), 1 };
    metadata.mode = 0040555;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}
KResultOr<size_t> DevFSRootDirectoryInode::directory_entry_count() const
{
    LOCKER(m_parent_fs.m_lock);
    return m_devices.size() + DevFSDirectoryInode::directory_entry_count().value();
}

DevFSDeviceInode::DevFSDeviceInode(DevFS& fs, const Device& device)
    : DevFSInode(fs)
    , m_attached_device(device)
{
}
DevFSDeviceInode::~DevFSDeviceInode()
{
}
KResult DevFSDeviceInode::chown(uid_t uid, gid_t gid)
{
    LOCKER(m_lock);
    m_uid = uid;
    m_gid = gid;
    return KSuccess;
}

String DevFSDeviceInode::name() const
{
    LOCKER(m_lock);
    if (m_cached_name.is_null() || m_cached_name.is_empty())
        const_cast<DevFSDeviceInode&>(*this).m_cached_name = m_attached_device->device_name();
    return m_cached_name;
}

ssize_t DevFSDeviceInode::read_bytes(off_t offset, ssize_t count, UserOrKernelBuffer& buffer, FileDescription* description) const
{
    LOCKER(m_lock);
    VERIFY(!!description);
    if (!m_attached_device->can_read(*description, offset))
        return -EIO;
    auto nread = const_cast<Device&>(*m_attached_device).read(*description, offset, buffer, count);
    if (nread.is_error())
        return -EIO;
    return nread.value();
}

InodeMetadata DevFSDeviceInode::metadata() const
{
    LOCKER(m_lock);
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
ssize_t DevFSDeviceInode::write_bytes(off_t offset, ssize_t count, const UserOrKernelBuffer& buffer, FileDescription* description)
{
    LOCKER(m_lock);
    VERIFY(!!description);
    if (!m_attached_device->can_write(*description, offset))
        return -EIO;
    auto nread = const_cast<Device&>(*m_attached_device).write(*description, offset, buffer, count);
    if (nread.is_error())
        return -EIO;
    return nread.value();
}

DevFSPtsDirectoryInode::DevFSPtsDirectoryInode(DevFS& fs)
    : DevFSDirectoryInode(fs)
{
}
KResult DevFSPtsDirectoryInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    LOCKER(m_lock);
    callback({ ".", identifier(), 0 });
    callback({ "..", identifier(), 0 });
    return KSuccess;
}
RefPtr<Inode> DevFSPtsDirectoryInode::lookup(StringView)
{
    return nullptr;
}
DevFSPtsDirectoryInode::~DevFSPtsDirectoryInode()
{
}
InodeMetadata DevFSPtsDirectoryInode::metadata() const
{
    LOCKER(m_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), index() };
    metadata.mode = 0040555;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}
KResultOr<size_t> DevFSPtsDirectoryInode::directory_entry_count() const
{
    return 0;
}

}
