/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Sections.h>

namespace Kernel {

static AK::Singleton<SysFSComponentRegistry> s_the;

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
    : m_root_folder(SysFSRootFolder::create())
{
}

UNMAP_AFTER_INIT void SysFSComponentRegistry::register_new_component(SysFSComponent& component)
{
    Locker locker(m_lock);
    m_root_folder->m_components.append(component);
}

NonnullRefPtr<SysFSRootFolder> SysFSRootFolder::create()
{
    return adopt_ref(*new (nothrow) SysFSRootFolder);
}

KResult SysFSRootFolder::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    Locker locker(SysFSComponentRegistry::the().m_lock);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, 0 }, 0 });

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    return KSuccess;
}

SysFSRootFolder::SysFSRootFolder()
    : SystemExposedFolder(".")
{
}

NonnullRefPtr<SysFS> SysFS::create()
{
    return adopt_ref(*new (nothrow) SysFS);
}

SysFS::SysFS()
    : m_root_inode(SysFSComponentRegistry::the().m_root_folder->to_inode(*this))
{
    Locker locker(m_lock);
}

SysFS::~SysFS()
{
}

bool SysFS::initialize()
{
    return true;
}

NonnullRefPtr<Inode> SysFS::root_inode() const
{
    return *m_root_inode;
}

NonnullRefPtr<SysFSInode> SysFSInode::create(SysFS const& fs, SysFSComponent const& component)
{
    return adopt_ref(*new (nothrow) SysFSInode(fs, component));
}

SysFSInode::SysFSInode(SysFS const& fs, SysFSComponent const& component)
    : Inode(const_cast<SysFS&>(fs), component.component_index())
    , m_associated_component(component)
{
}

KResultOr<size_t> SysFSInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* fd) const
{
    return m_associated_component->read_bytes(offset, count, buffer, fd);
}

KResult SysFSInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

RefPtr<Inode> SysFSInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata SysFSInode::metadata() const
{
    Locker locker(m_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFREG | S_IRUSR | S_IRGRP | S_IROTH;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = m_associated_component->size();
    metadata.mtime = mepoch;
    return metadata;
}

void SysFSInode::flush_metadata()
{
}

KResultOr<size_t> SysFSInode::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& buffer, FileDescription* fd)
{
    return m_associated_component->write_bytes(offset, count, buffer, fd);
}

KResultOr<NonnullRefPtr<Inode>> SysFSInode::create_child(String const&, mode_t, dev_t, uid_t, gid_t)
{
    return EROFS;
}

KResult SysFSInode::add_child(Inode&, StringView const&, mode_t)
{
    return EROFS;
}

KResult SysFSInode::remove_child(StringView const&)
{
    return EROFS;
}

KResultOr<size_t> SysFSInode::directory_entry_count() const
{
    VERIFY_NOT_REACHED();
}

KResult SysFSInode::chmod(mode_t)
{
    return EPERM;
}

KResult SysFSInode::chown(uid_t, gid_t)
{
    return EPERM;
}

KResult SysFSInode::truncate(u64)
{
    return EPERM;
}

NonnullRefPtr<SysFSDirectoryInode> SysFSDirectoryInode::create(SysFS const& sysfs, SysFSComponent const& component)
{
    return adopt_ref(*new (nothrow) SysFSDirectoryInode(sysfs, component));
}

SysFSDirectoryInode::SysFSDirectoryInode(SysFS const& fs, SysFSComponent const& component)
    : SysFSInode(fs, component)
    , m_parent_fs(const_cast<SysFS&>(fs))
{
}

SysFSDirectoryInode::~SysFSDirectoryInode()
{
}
InodeMetadata SysFSDirectoryInode::metadata() const
{
    Locker locker(m_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFDIR | S_IRUSR | S_IRGRP | S_IROTH | S_IXOTH;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}
KResult SysFSDirectoryInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    Locker locker(m_parent_fs.m_lock);
    return m_associated_component->traverse_as_directory(m_parent_fs.fsid(), move(callback));
}

RefPtr<Inode> SysFSDirectoryInode::lookup(StringView name)
{
    Locker locker(m_parent_fs.m_lock);
    auto component = m_associated_component->lookup(name);
    if (!component)
        return {};
    return component->to_inode(m_parent_fs);
}

KResultOr<size_t> SysFSDirectoryInode::directory_entry_count() const
{
    Locker locker(m_lock);
    return m_associated_component->entries_count();
}

}
