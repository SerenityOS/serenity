/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/DevTmpFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>

namespace Kernel {

KResultOr<NonnullRefPtr<DevTmpFS>> DevTmpFS::try_create()
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) DevTmpFS);
}

DevTmpFS::DevTmpFS()
{
}

size_t DevTmpFS::allocate_inode_index()
{
    MutexLocker locker(m_lock);
    m_next_inode_index = m_next_inode_index.value() + 1;
    VERIFY(m_next_inode_index > 0);
    return 1 + m_next_inode_index.value();
}

DevTmpFS::~DevTmpFS()
{
}

KResult DevTmpFS::initialize()
{
    m_root_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevTmpFSRootDirectoryInode(*this)));
    return KSuccess;
}

Inode& DevTmpFS::root_inode()
{
    return *m_root_inode;
}

DevTmpFSInode::DevTmpFSInode(DevTmpFS& fs)
    : Inode(fs, fs.allocate_inode_index())
{
}

KResultOr<size_t> DevTmpFSInode::read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    VERIFY_NOT_REACHED();
}

KResult DevTmpFSInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

KResultOr<NonnullRefPtr<Inode>> DevTmpFSInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

void DevTmpFSInode::flush_metadata()
{
}

KResultOr<size_t> DevTmpFSInode::write_bytes(off_t, size_t, const UserOrKernelBuffer&, OpenFileDescription*)
{
    VERIFY_NOT_REACHED();
}

KResultOr<NonnullRefPtr<Inode>> DevTmpFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

KResult DevTmpFSInode::add_child(Inode&, const StringView&, mode_t)
{
    return EROFS;
}

KResult DevTmpFSInode::remove_child(const StringView&)
{
    return EROFS;
}

KResult DevTmpFSInode::chmod(mode_t)
{
    return EPERM;
}

KResult DevTmpFSInode::chown(UserID, GroupID)
{
    return EPERM;
}

KResult DevTmpFSInode::truncate(u64)
{
    return EPERM;
}

StringView DevTmpFSLinkInode::name() const
{
    return m_name->view();
}

DevTmpFSLinkInode::~DevTmpFSLinkInode()
{
}

DevTmpFSLinkInode::DevTmpFSLinkInode(DevTmpFS& fs, NonnullOwnPtr<KString> name)
    : DevTmpFSInode(fs)
    , m_name(move(name))
{
}

KResultOr<size_t> DevTmpFSLinkInode::read_bytes(off_t offset, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    MutexLocker locker(m_inode_lock);
    VERIFY(offset == 0);
    VERIFY(m_link);
    TRY(buffer.write(m_link->characters() + offset, m_link->length()));
    return m_link->length();
}

InodeMetadata DevTmpFSLinkInode::metadata() const
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

KResultOr<size_t> DevTmpFSLinkInode::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription*)
{
    auto new_string = TRY(buffer.try_copy_into_kstring(count));

    MutexLocker locker(m_inode_lock);
    VERIFY(offset == 0);
    VERIFY(buffer.is_kernel_buffer());
    m_link = move(new_string);
    return count;
}

DevTmpFSDirectoryInode::DevTmpFSDirectoryInode(DevTmpFS& fs)
    : DevTmpFSInode(fs)
{
}
DevTmpFSDirectoryInode::~DevTmpFSDirectoryInode()
{
}

InodeMetadata DevTmpFSDirectoryInode::metadata() const
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

DevTmpFSRootDirectoryInode::DevTmpFSRootDirectoryInode(DevTmpFS& fs)
    : DevTmpFSDirectoryInode(fs)
{
}

KResult DevTmpFSRootDirectoryInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(fs().m_lock);
    callback({ ".", identifier(), 0 });
    callback({ "..", identifier(), 0 });
    for (auto& node : m_nodes) {
        InodeIdentifier identifier = { fsid(), node.index() };
        callback({ node.name(), identifier, 0 });
    }
    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> DevTmpFSRootDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(fs().m_lock);
    for (auto& node : m_nodes) {
        if (node.name() == name)
            return node;
    }
    return ENOENT;
}

KResult DevTmpFSRootDirectoryInode::remove_child(const StringView& name)
{
    MutexLocker locker(fs().m_lock);
    for (auto& node : m_nodes) {
        if (node.name() == name) {
            m_nodes.remove(node);
            return KSuccess;
        }
    }
    return KResult(ENOENT);
}

KResultOr<NonnullRefPtr<Inode>> DevTmpFSRootDirectoryInode::create_child(StringView name, mode_t mode, dev_t device_mode, UserID, GroupID)
{
    MutexLocker locker(fs().m_lock);

    for (auto& node : m_nodes) {
        if (node.name() == name)
            return KResult(EEXIST);
    }

    InodeMetadata metadata;
    metadata.mode = mode;
    if (metadata.is_directory()) {
        if (name != "pts")
            return EROFS;
        auto new_directory_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevTmpFSPtsDirectoryInode(fs())));
        m_nodes.append(*new_directory_inode);
        return new_directory_inode;
    }
    if (metadata.is_device()) {
        auto name_kstring = TRY(KString::try_create(name));
        unsigned major = major_from_encoded_device(device_mode);
        unsigned minor = minor_from_encoded_device(device_mode);
        auto new_device_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevTmpFSDeviceInode(fs(), major, minor, is_block_device(mode), move(name_kstring))));
        TRY(new_device_inode->chmod(mode));
        m_nodes.append(*new_device_inode);
        return new_device_inode;
    }
    if (metadata.is_symlink()) {
        auto name_kstring = TRY(KString::try_create(name));
        auto new_link_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevTmpFSLinkInode(fs(), move(name_kstring))));
        m_nodes.append(*new_link_inode);
        return new_link_inode;
    }
    return EROFS;
}

DevTmpFSRootDirectoryInode::~DevTmpFSRootDirectoryInode()
{
}
InodeMetadata DevTmpFSRootDirectoryInode::metadata() const
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

DevTmpFSDeviceInode::DevTmpFSDeviceInode(DevTmpFS& fs, unsigned major_number, unsigned minor_number, bool block_device, NonnullOwnPtr<KString> name)
    : DevTmpFSInode(fs)
    , m_name(move(name))
    , m_major_number(major_number)
    , m_minor_number(minor_number)
    , m_block_device(block_device)
{
}

DevTmpFSDeviceInode::~DevTmpFSDeviceInode()
{
}

KResult DevTmpFSDeviceInode::chown(UserID uid, GroupID gid)
{
    MutexLocker locker(m_inode_lock);
    m_uid = uid;
    m_gid = gid;
    return KSuccess;
}

KResult DevTmpFSDeviceInode::chmod(mode_t mode)
{
    MutexLocker locker(m_inode_lock);
    mode &= 0777;
    if (m_required_mode == mode)
        return KSuccess;
    m_required_mode = mode;
    return KSuccess;
}

StringView DevTmpFSDeviceInode::name() const
{
    return m_name->view();
}

KResultOr<size_t> DevTmpFSDeviceInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    MutexLocker locker(m_inode_lock);
    VERIFY(!!description);
    RefPtr<Device> device = Device::get_device(m_major_number, m_minor_number);
    if (!device)
        return KResult(ENODEV);
    if (!device->can_read(*description, offset))
        return KResult(ENOTIMPL);
    auto result = const_cast<Device&>(*device).read(*description, offset, buffer, count);
    if (result.is_error())
        return result;
    return result.value();
}

InodeMetadata DevTmpFSDeviceInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), index() };
    metadata.mode = (m_block_device ? S_IFBLK : S_IFCHR) | m_required_mode;
    metadata.uid = m_uid;
    metadata.gid = m_gid;
    metadata.size = 0;
    metadata.mtime = mepoch;
    metadata.major_device = m_major_number;
    metadata.minor_device = m_minor_number;
    return metadata;
}
KResultOr<size_t> DevTmpFSDeviceInode::write_bytes(off_t offset, size_t count, const UserOrKernelBuffer& buffer, OpenFileDescription* description)
{
    MutexLocker locker(m_inode_lock);
    VERIFY(!!description);
    RefPtr<Device> device = Device::get_device(m_major_number, m_minor_number);
    if (!device)
        return KResult(ENODEV);
    if (!device->can_write(*description, offset))
        return KResult(ENOTIMPL);
    auto result = const_cast<Device&>(*device).write(*description, offset, buffer, count);
    if (result.is_error())
        return result;
    return result.value();
}

DevTmpFSPtsDirectoryInode::DevTmpFSPtsDirectoryInode(DevTmpFS& fs)
    : DevTmpFSDirectoryInode(fs)
{
}
KResult DevTmpFSPtsDirectoryInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(m_inode_lock);
    callback({ ".", identifier(), 0 });
    callback({ "..", identifier(), 0 });
    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> DevTmpFSPtsDirectoryInode::lookup(StringView)
{
    return ENOENT;
}

DevTmpFSPtsDirectoryInode::~DevTmpFSPtsDirectoryInode()
{
}
InodeMetadata DevTmpFSPtsDirectoryInode::metadata() const
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
