/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<SysFSComponentRegistry> s_the;

SysFSComponentRegistry& SysFSComponentRegistry::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT void SysFSComponentRegistry::initialize()
{
    VERIFY(!s_the.is_initialized());
    s_the.ensure_instance();
}

UNMAP_AFTER_INIT SysFSComponentRegistry::SysFSComponentRegistry()
    : m_root_directory(SysFSRootDirectory::create())
{
}

UNMAP_AFTER_INIT void SysFSComponentRegistry::register_new_component(SysFSComponent& component)
{
    MutexLocker locker(m_lock);
    m_root_directory->m_components.append(component);
}

SysFSComponentRegistry::DevicesList& SysFSComponentRegistry::devices_list()
{
    return m_devices_list;
}

NonnullRefPtr<SysFSRootDirectory> SysFSRootDirectory::create()
{
    return adopt_ref(*new (nothrow) SysFSRootDirectory);
}

ErrorOr<void> SysFSRootDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(SysFSComponentRegistry::the().get_lock());
    TRY(callback({ ".", { fsid, component_index() }, 0 }));
    TRY(callback({ "..", { fsid, 0 }, 0 }));

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        TRY(callback({ component.name(), identifier, 0 }));
    }
    return {};
}

SysFSRootDirectory::SysFSRootDirectory()
{
    auto buses_directory = SysFSBusDirectory::must_create(*this);
    auto devices_directory = SysFSDevicesDirectory::must_create(*this);
    m_components.append(buses_directory);
    m_components.append(devices_directory);
    m_buses_directory = buses_directory;
}

ErrorOr<NonnullRefPtr<SysFS>> SysFS::try_create()
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFS);
}

SysFS::SysFS()
{
}

SysFS::~SysFS()
{
}

ErrorOr<void> SysFS::initialize()
{
    m_root_inode = TRY(SysFSComponentRegistry::the().root_directory().to_inode(*this));
    return {};
}

Inode& SysFS::root_inode()
{
    return *m_root_inode;
}

ErrorOr<NonnullRefPtr<SysFSInode>> SysFSInode::try_create(SysFS const& fs, SysFSComponent const& component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSInode(fs, component));
}

SysFSInode::SysFSInode(SysFS const& fs, SysFSComponent const& component)
    : Inode(const_cast<SysFS&>(fs), component.component_index())
    , m_associated_component(component)
{
}

void SysFSInode::did_seek(OpenFileDescription& description, off_t new_offset)
{
    if (new_offset != 0)
        return;
    auto result = m_associated_component->refresh_data(description);
    if (result.is_error()) {
        // Subsequent calls to read will return EIO!
        dbgln("SysFS: Could not refresh contents: {}", result.error());
    }
}

ErrorOr<void> SysFSInode::attach(OpenFileDescription& description)
{
    return m_associated_component->refresh_data(description);
}

ErrorOr<size_t> SysFSInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* fd) const
{
    return m_associated_component->read_bytes(offset, count, buffer, fd);
}

ErrorOr<void> SysFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullRefPtr<Inode>> SysFSInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata SysFSInode::metadata() const
{
    // NOTE: No locking required as m_associated_component or its component index will never change during our lifetime.
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFREG | m_associated_component->permissions();
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}

ErrorOr<void> SysFSInode::flush_metadata()
{
    return {};
}

ErrorOr<size_t> SysFSInode::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription* fd)
{
    return m_associated_component->write_bytes(offset, count, buffer, fd);
}

ErrorOr<NonnullRefPtr<Inode>> SysFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> SysFSInode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<void> SysFSInode::remove_child(StringView)
{
    return EROFS;
}

ErrorOr<void> SysFSInode::chmod(mode_t)
{
    return EPERM;
}

ErrorOr<void> SysFSInode::chown(UserID, GroupID)
{
    return EPERM;
}

ErrorOr<void> SysFSInode::set_mtime(time_t time)
{
    return m_associated_component->set_mtime(time);
}

ErrorOr<void> SysFSInode::truncate(u64 size)
{
    return m_associated_component->truncate(size);
}

ErrorOr<NonnullRefPtr<SysFSDirectoryInode>> SysFSDirectoryInode::try_create(SysFS const& sysfs, SysFSComponent const& component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSDirectoryInode(sysfs, component));
}

SysFSDirectoryInode::SysFSDirectoryInode(SysFS const& fs, SysFSComponent const& component)
    : SysFSInode(fs, component)
{
}

SysFSDirectoryInode::~SysFSDirectoryInode()
{
}

InodeMetadata SysFSDirectoryInode::metadata() const
{
    // NOTE: No locking required as m_associated_component or its component index will never change during our lifetime.
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFDIR | S_IRUSR | S_IRGRP | S_IROTH | S_IXOTH;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}
ErrorOr<void> SysFSDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(fs().m_lock);
    return m_associated_component->traverse_as_directory(fs().fsid(), move(callback));
}

ErrorOr<NonnullRefPtr<Inode>> SysFSDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(fs().m_lock);
    auto component = m_associated_component->lookup(name);
    if (!component)
        return ENOENT;
    return TRY(component->to_inode(fs()));
}

SysFSBusDirectory& SysFSComponentRegistry::buses_directory()
{
    return *m_root_directory->m_buses_directory;
}

void SysFSComponentRegistry::register_new_bus_directory(SysFSDirectory& new_bus_directory)
{
    VERIFY(!m_root_directory->m_buses_directory.is_null());
    m_root_directory->m_buses_directory->m_components.append(new_bus_directory);
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSBusDirectory> SysFSBusDirectory::must_create(SysFSRootDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSBusDirectory(parent_directory));
    return directory;
}

UNMAP_AFTER_INIT SysFSBusDirectory::SysFSBusDirectory(SysFSRootDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
