/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Sections.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

static AK::Singleton<ProcFSComponentRegistry> s_the;

ProcFSComponentRegistry& ProcFSComponentRegistry::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT void ProcFSComponentRegistry::initialize()
{
    VERIFY(!s_the.is_initialized());
    s_the.ensure_instance();
}

UNMAP_AFTER_INIT ProcFSComponentRegistry::ProcFSComponentRegistry()
    : m_root_directory(ProcFSRootDirectory::must_create())
{
}

void ProcFSComponentRegistry::register_new_process(Process& new_process)
{
    Locker locker(m_lock);
    m_root_directory->m_process_directories.append(ProcFSProcessDirectory::create(new_process));
}

void ProcFSComponentRegistry::unregister_process(Process& deleted_process)
{
    auto process_directory = m_root_directory->process_directory_for(deleted_process).release_nonnull();
    process_directory->prepare_for_deletion();
    process_directory->m_list_node.remove();
    dbgln_if(PROCFS_DEBUG, "ProcFSExposedDirectory ref_count now: {}", process_directory->ref_count());
}

RefPtr<ProcFS> ProcFS::create()
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFS);
}

ProcFS::~ProcFS()
{
}

bool ProcFS::initialize()
{
    return true;
}

NonnullRefPtr<Inode> ProcFS::root_inode() const
{
    return *m_root_inode;
}

NonnullRefPtr<ProcFSInode> ProcFSInode::create(const ProcFS& fs, const ProcFSExposedComponent& component)
{
    return adopt_ref(*new (nothrow) ProcFSInode(fs, component));
}

ProcFSInode::ProcFSInode(const ProcFS& fs, const ProcFSExposedComponent& component)
    : Inode(const_cast<ProcFS&>(fs), component.component_index())
    , m_associated_component(component)
{
}

KResult ProcFSInode::attach(FileDescription& description)
{
    return m_associated_component->refresh_data(description);
}

void ProcFSInode::did_seek(FileDescription& description, off_t new_offset)
{
    if (new_offset != 0)
        return;
    auto result = m_associated_component->refresh_data(description);
    if (result.is_error()) {
        // Subsequent calls to read will return EIO!
        dbgln("ProcFS: Could not refresh contents: {}", result.error());
    }
}

ProcFSInode::~ProcFSInode()
{
}

ProcFS::ProcFS()
    : m_root_inode(ProcFSComponentRegistry::the().root_directory().to_inode(*this))
{
}

KResultOr<size_t> ProcFSInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* fd) const
{
    return m_associated_component->read_bytes(offset, count, buffer, fd);
}

StringView ProcFSInode::name() const
{
    return m_associated_component->name();
}

KResult ProcFSInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

RefPtr<Inode> ProcFSInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata ProcFSInode::metadata() const
{
    Locker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = m_associated_component->required_mode();
    metadata.uid = m_associated_component->owner_user();
    metadata.gid = m_associated_component->owner_group();
    metadata.size = m_associated_component->size();
    metadata.mtime = m_associated_component->modified_time();
    return metadata;
}

void ProcFSInode::flush_metadata()
{
}

KResultOr<size_t> ProcFSInode::write_bytes(off_t offset, size_t count, const UserOrKernelBuffer& buffer, FileDescription* fd)
{
    return m_associated_component->write_bytes(offset, count, buffer, fd);
}

KResultOr<NonnullRefPtr<Inode>> ProcFSInode::create_child(StringView, mode_t, dev_t, uid_t, gid_t)
{
    return EROFS;
}

KResult ProcFSInode::add_child(Inode&, const StringView&, mode_t)
{
    return EROFS;
}

KResult ProcFSInode::remove_child(const StringView&)
{
    return EROFS;
}

KResult ProcFSInode::chmod(mode_t)
{
    return EPERM;
}

KResult ProcFSInode::chown(uid_t, gid_t)
{
    return EPERM;
}

KResult ProcFSInode::truncate(u64)
{
    return EPERM;
}

NonnullRefPtr<ProcFSDirectoryInode> ProcFSDirectoryInode::create(const ProcFS& procfs, const ProcFSExposedComponent& component)
{
    return adopt_ref(*new (nothrow) ProcFSDirectoryInode(procfs, component));
}

ProcFSDirectoryInode::ProcFSDirectoryInode(const ProcFS& fs, const ProcFSExposedComponent& component)
    : ProcFSInode(fs, component)
    , m_parent_fs(const_cast<ProcFS&>(fs))
{
}

ProcFSDirectoryInode::~ProcFSDirectoryInode()
{
}
InodeMetadata ProcFSDirectoryInode::metadata() const
{
    Locker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFDIR | m_associated_component->required_mode();
    metadata.uid = m_associated_component->owner_user();
    metadata.gid = m_associated_component->owner_group();
    metadata.size = 0;
    metadata.mtime = m_associated_component->modified_time();
    return metadata;
}
KResult ProcFSDirectoryInode::traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    Locker locker(m_parent_fs.m_lock);
    return m_associated_component->traverse_as_directory(m_parent_fs.fsid(), move(callback));
}

RefPtr<Inode> ProcFSDirectoryInode::lookup(StringView name)
{
    Locker locker(m_parent_fs.m_lock);
    auto component = m_associated_component->lookup(name);
    if (!component)
        return {};
    return component->to_inode(m_parent_fs);
}

NonnullRefPtr<ProcFSLinkInode> ProcFSLinkInode::create(const ProcFS& procfs, const ProcFSExposedComponent& component)
{
    return adopt_ref(*new (nothrow) ProcFSLinkInode(procfs, component));
}

ProcFSLinkInode::ProcFSLinkInode(const ProcFS& fs, const ProcFSExposedComponent& component)
    : ProcFSInode(fs, component)
{
}
InodeMetadata ProcFSLinkInode::metadata() const
{
    Locker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFLNK | m_associated_component->required_mode();
    metadata.uid = m_associated_component->owner_user();
    metadata.gid = m_associated_component->owner_group();
    metadata.size = 0;
    metadata.mtime = m_associated_component->modified_time();
    return metadata;
}

}
