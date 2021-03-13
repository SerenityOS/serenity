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
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>

namespace Kernel {

static InodeIndex s_next_inode_index;
static AK::Singleton<SystemRegistrar> s_the;

SystemRegistrar& SystemRegistrar::the()
{
    return *s_the;
}

void SystemRegistrar::initialize()
{
    VERIFY(!s_the.is_initialized());
    s_next_inode_index = 0;
    s_the.ensure_instance();
}

static size_t allocate_inode_index()
{
    //LOCKER(s_lock);
    s_next_inode_index = s_next_inode_index.value() + 1;
    VERIFY(s_next_inode_index > 0);
    return s_next_inode_index.value();
}

SystemRegistrar::SystemRegistrar()
    : m_root_folder(SysFSRootFolder::create())
{
}

RefPtr<SysFSExposedComponent> SystemRegistrar::get_component_by_index(InodeIndex index) const
{
    if (index == 1)
        return m_root_folder;
    for (auto& component : m_sysfs_components) {
        if (static_cast<InodeIndex>(component.component_index()) == index)
            return component;
    }
    return {};
}

void SystemRegistrar::register_new_component(SysFSExposedComponent& component)
{
    LOCKER(m_lock);
    m_root_folder->m_components.append(component);
}

SysFSExposedComponent::SysFSExposedComponent(String name)
    : m_name(name)
    , m_component_index(allocate_inode_index())
{
}

NonnullRefPtr<SysFSRootFolder> SysFSRootFolder::create()
{
    return adopt(*new SysFSRootFolder);
}

KResult SysFSRootFolder::traverse_as_directory(const SysFS& calling_sysfs_instance, Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    LOCKER(SystemRegistrar::the().m_lock);
    callback({ ".", { calling_sysfs_instance.fsid(), component_index() }, 0 });
    callback({ "..", { calling_sysfs_instance.fsid(), 0 }, 0 });

    for (auto& component : m_components) {
        InodeIdentifier identifier = { calling_sysfs_instance.fsid(), component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    return KSuccess;
}

SysFSRootFolder::SysFSRootFolder()
    : SysFSExposedFolder(".")
{
}

NonnullRefPtr<SysFSInode> SysFSExposedFolder::to_inode(const SysFS& sysfs_instance) const
{
    return SysFSDirectoryInode::create(sysfs_instance, *this);
}

KResult SysFSExposedFolder::traverse_as_directory(const SysFS& calling_sysfs_instance, Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    LOCKER(SystemRegistrar::the().m_lock);
    VERIFY(m_parent_folder);
    callback({ ".", { calling_sysfs_instance.fsid(), component_index() }, 0 });
    callback({ "..", { calling_sysfs_instance.fsid(), m_parent_folder->component_index() }, 0 });

    for (auto& component : m_components) {
        InodeIdentifier identifier = { calling_sysfs_instance.fsid(), component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    return KSuccess;
}

RefPtr<SysFSExposedComponent> SysFSExposedFolder::lookup(StringView name)
{
    for (auto& component : m_components) {
        if (component.name() == name) {
            return component;
        }
    }
    return {};
}

SysFSExposedFolder::SysFSExposedFolder(String name)
    : SysFSExposedComponent(name)
{
}

SysFSExposedFolder::SysFSExposedFolder(String name, SysFSExposedFolder& parent_folder)
    : SysFSExposedComponent(name)
    , m_parent_folder(parent_folder)
{
}

NonnullRefPtr<SysFSInode> SysFSExposedComponent::to_inode(const SysFS& sysfs_instance) const
{
    return SysFSInode::create(sysfs_instance, *this);
}

NonnullRefPtr<SysFS> SysFS::create()
{
    return adopt(*new SysFS);
}

SysFS::SysFS()
    : m_root_inode(SystemRegistrar::the().m_root_folder->to_inode(*this))
{
    LOCKER(m_lock);
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

RefPtr<Inode> SysFS::get_inode(InodeIdentifier inode_id) const
{
    LOCKER(m_lock);
    auto component = SystemRegistrar::the().get_component_by_index(inode_id.index());
    if (!component)
        return {};
    return component->to_inode(*this);
}

NonnullRefPtr<SysFSInode> SysFSInode::create(const SysFS& fs, const SysFSExposedComponent& component)
{
    return adopt(*new SysFSInode(fs, component));
}

SysFSInode::SysFSInode(const SysFS& fs, const SysFSExposedComponent& component)
    : Inode(const_cast<SysFS&>(fs), component.component_index())
    , m_associated_component(component)
{
}

ssize_t SysFSInode::read_bytes(off_t offset, ssize_t count, UserOrKernelBuffer& buffer, FileDescription* fd) const
{
    return m_associated_component->read_bytes(offset, count, buffer, fd);
}

KResult SysFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)>) const
{
    VERIFY_NOT_REACHED();
}

RefPtr<Inode> SysFSInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata SysFSInode::metadata() const
{
    LOCKER(m_lock);
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

ssize_t SysFSInode::write_bytes(off_t offset, ssize_t count, const UserOrKernelBuffer& buffer, FileDescription* fd)
{
    return m_associated_component->write_bytes(offset, count, buffer, fd);
}

KResultOr<NonnullRefPtr<Inode>> SysFSInode::create_child(const String&, mode_t, dev_t, uid_t, gid_t)
{
    return EROFS;
}

KResult SysFSInode::add_child(Inode&, const StringView&, mode_t)
{
    return EROFS;
}

KResult SysFSInode::remove_child(const StringView&)
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

NonnullRefPtr<SysFSDirectoryInode> SysFSDirectoryInode::create(const SysFS& sysfs, const SysFSExposedComponent& component)
{
    return adopt(*new SysFSDirectoryInode(sysfs, component));
}

SysFSDirectoryInode::SysFSDirectoryInode(const SysFS& fs, const SysFSExposedComponent& component)
    : SysFSInode(fs, component)
    , m_parent_fs(const_cast<SysFS&>(fs))
{
}

SysFSDirectoryInode::~SysFSDirectoryInode()
{
}
InodeMetadata SysFSDirectoryInode::metadata() const
{
    LOCKER(m_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFDIR | S_IRUSR | S_IRGRP | S_IROTH | S_IXOTH;
    metadata.uid = 0;
    metadata.gid = 0;
    metadata.size = 0;
    metadata.mtime = mepoch;
    return metadata;
}
KResult SysFSDirectoryInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    LOCKER(m_parent_fs.m_lock);
    return m_associated_component->traverse_as_directory(m_parent_fs, move(callback));
}

RefPtr<Inode> SysFSDirectoryInode::lookup(StringView name)
{
    LOCKER(m_parent_fs.m_lock);
    auto component = m_associated_component->lookup(name);
    if (!component)
        return {};
    return component->to_inode(m_parent_fs);
}

KResultOr<size_t> SysFSDirectoryInode::directory_entry_count() const
{
    LOCKER(m_lock);
    return m_associated_component->entries_count();
}

}
