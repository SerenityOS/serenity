/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/DevTmpFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<DevTmpFS>> DevTmpFS::try_create()
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

ErrorOr<void> DevTmpFS::initialize()
{
    m_root_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevTmpFSRootDirectoryInode(*this)));
    return {};
}

Inode& DevTmpFS::root_inode()
{
    return *m_root_inode;
}

DevTmpFSInode::DevTmpFSInode(DevTmpFS& fs)
    : Inode(fs, fs.allocate_inode_index())
{
}

DevTmpFSInode::DevTmpFSInode(DevTmpFS& fs, MajorNumber major_number, MinorNumber minor_number)
    : Inode(fs, fs.allocate_inode_index())
    , m_major_number(major_number)
    , m_minor_number(minor_number)
{
}

ErrorOr<size_t> DevTmpFSInode::read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> DevTmpFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullRefPtr<Inode>> DevTmpFSInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> DevTmpFSInode::flush_metadata()
{
    return {};
}

ErrorOr<size_t> DevTmpFSInode::write_bytes(off_t, size_t, const UserOrKernelBuffer&, OpenFileDescription*)
{
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullRefPtr<Inode>> DevTmpFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> DevTmpFSInode::add_child(Inode&, StringView, mode_t)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata DevTmpFSInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    VERIFY((m_mode & 0777) == m_mode);
    InodeMetadata metadata;
    metadata.uid = m_uid;
    metadata.gid = m_gid;
    metadata.size = 0;
    metadata.mtime = mepoch;
    switch (node_type()) {
    case Type::RootDirectory:
        metadata.inode = { fsid(), 1 };
        metadata.mode = 0040555;
        metadata.uid = 0;
        metadata.gid = 0;
        metadata.size = 0;
        metadata.mtime = mepoch;
        break;
    case Type::Directory:
        metadata.inode = { fsid(), index() };
        metadata.mode = S_IFDIR | m_mode;
        break;
    case Type::BlockDevice:
        metadata.inode = { fsid(), index() };
        metadata.mode = S_IFBLK | m_mode;
        metadata.major_device = m_major_number;
        metadata.minor_device = m_minor_number;
        break;
    case Type::CharacterDevice:
        metadata.inode = { fsid(), index() };
        metadata.mode = S_IFCHR | m_mode;
        metadata.major_device = m_major_number;
        metadata.minor_device = m_minor_number;
        break;
    case Type::Link:
        metadata.inode = { fsid(), index() };
        metadata.mode = S_IFLNK | m_mode;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    return metadata;
}

ErrorOr<void> DevTmpFSInode::remove_child(StringView)
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> DevTmpFSInode::chmod(mode_t mode)
{
    MutexLocker locker(m_inode_lock);
    mode &= 0777;
    if (m_mode == mode)
        return {};
    m_mode = mode;
    return {};
}

ErrorOr<void> DevTmpFSInode::chown(UserID uid, GroupID gid)
{
    MutexLocker locker(m_inode_lock);
    m_uid = uid;
    m_gid = gid;
    return {};
}

ErrorOr<void> DevTmpFSInode::truncate(u64)
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

ErrorOr<size_t> DevTmpFSLinkInode::read_bytes(off_t offset, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    MutexLocker locker(m_inode_lock);
    VERIFY(offset == 0);
    VERIFY(m_link);
    TRY(buffer.write(m_link->characters() + offset, m_link->length()));
    return m_link->length();
}

ErrorOr<size_t> DevTmpFSLinkInode::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription*)
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
DevTmpFSDirectoryInode::DevTmpFSDirectoryInode(DevTmpFS& fs, NonnullOwnPtr<KString> name)
    : DevTmpFSInode(fs)
    , m_name(move(name))
{
}
DevTmpFSDirectoryInode::~DevTmpFSDirectoryInode()
{
}

ErrorOr<void> DevTmpFSDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(m_inode_lock);
    TRY(callback({ ".", identifier(), 0 }));
    TRY(callback({ "..", identifier(), 0 }));
    for (auto& node : m_nodes) {
        InodeIdentifier identifier = { fsid(), node.index() };
        TRY(callback({ node.name(), identifier, 0 }));
    }
    return {};
}

ErrorOr<NonnullRefPtr<Inode>> DevTmpFSDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(m_inode_lock);
    for (auto& node : m_nodes) {
        if (node.name() == name) {
            return node;
        }
    }
    return Error::from_errno(ENOENT);
}

ErrorOr<void> DevTmpFSDirectoryInode::remove_child(StringView name)
{
    MutexLocker locker(m_inode_lock);
    for (auto& node : m_nodes) {
        if (node.name() == name) {
            m_nodes.remove(node);
            return {};
        }
    }
    return Error::from_errno(ENOENT);
}

ErrorOr<NonnullRefPtr<Inode>> DevTmpFSDirectoryInode::create_child(StringView name, mode_t mode, dev_t device_mode, UserID, GroupID)
{
    MutexLocker locker(m_inode_lock);
    for (auto& node : m_nodes) {
        if (node.name() == name)
            return Error::from_errno(EEXIST);
    }

    InodeMetadata metadata;
    metadata.mode = mode;
    if (metadata.is_directory()) {
        auto name_kstring = TRY(KString::try_create(name));
        auto new_directory_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevTmpFSDirectoryInode(fs(), move(name_kstring))));
        m_nodes.append(*new_directory_inode);
        return new_directory_inode;
    }
    if (metadata.is_device()) {
        auto name_kstring = TRY(KString::try_create(name));
        auto major = major_from_encoded_device(device_mode);
        auto minor = minor_from_encoded_device(device_mode);
        auto new_device_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevTmpFSDeviceInode(fs(), major, minor, is_block_device(mode), move(name_kstring))));
        TRY(new_device_inode->chmod(mode));
        m_nodes.append(*new_device_inode);
        return new_device_inode;
    }
    if (metadata.is_symlink()) {
        auto name_kstring = TRY(KString::try_create(name));
        auto new_link_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DevTmpFSLinkInode(fs(), move(name_kstring))));
        TRY(new_link_inode->chmod(mode));
        m_nodes.append(*new_link_inode);
        return new_link_inode;
    }
    return EROFS;
}

DevTmpFSRootDirectoryInode::DevTmpFSRootDirectoryInode(DevTmpFS& fs)
    : DevTmpFSDirectoryInode(fs)
{
    m_mode = 0555;
}
DevTmpFSRootDirectoryInode::~DevTmpFSRootDirectoryInode()
{
}
ErrorOr<void> DevTmpFSRootDirectoryInode::chmod(mode_t)
{
    return EPERM;
}

ErrorOr<void> DevTmpFSRootDirectoryInode::chown(UserID, GroupID)
{
    return EPERM;
}

DevTmpFSDeviceInode::DevTmpFSDeviceInode(DevTmpFS& fs, MajorNumber major_number, MinorNumber minor_number, bool block_device, NonnullOwnPtr<KString> name)
    : DevTmpFSInode(fs, major_number, minor_number)
    , m_name(move(name))
    , m_block_device(block_device)
{
}

DevTmpFSDeviceInode::~DevTmpFSDeviceInode()
{
}

StringView DevTmpFSDeviceInode::name() const
{
    return m_name->view();
}

ErrorOr<size_t> DevTmpFSDeviceInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    MutexLocker locker(m_inode_lock);
    VERIFY(!!description);
    RefPtr<Device> device = DeviceManagement::the().get_device(m_major_number, m_minor_number);
    if (!device)
        return Error::from_errno(ENODEV);
    if (!device->can_read(*description, offset))
        return Error::from_errno(ENOTIMPL);
    auto result = const_cast<Device&>(*device).read(*description, offset, buffer, count);
    if (result.is_error())
        return result;
    return result.value();
}

ErrorOr<size_t> DevTmpFSDeviceInode::write_bytes(off_t offset, size_t count, const UserOrKernelBuffer& buffer, OpenFileDescription* description)
{
    MutexLocker locker(m_inode_lock);
    VERIFY(!!description);
    RefPtr<Device> device = DeviceManagement::the().get_device(m_major_number, m_minor_number);
    if (!device)
        return Error::from_errno(ENODEV);
    if (!device->can_write(*description, offset))
        return Error::from_errno(ENOTIMPL);
    auto result = const_cast<Device&>(*device).write(*description, offset, buffer, count);
    if (result.is_error())
        return result;
    return result.value();
}

}
