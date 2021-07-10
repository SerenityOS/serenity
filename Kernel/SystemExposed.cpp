/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/SystemExposed.h>

namespace Kernel {

static SpinLock<u8> s_index_lock;
static InodeIndex s_next_inode_index { 0 };

static size_t allocate_inode_index()
{
    ScopedSpinLock lock(s_index_lock);
    s_next_inode_index = s_next_inode_index.value() + 1;
    VERIFY(s_next_inode_index > 0);
    return s_next_inode_index.value();
}

SystemExposedComponent::SystemExposedComponent(StringView name)
    : m_name(KString::try_create(name).release_nonnull())
    , m_component_index(allocate_inode_index())
{
}

KResult SystemExposedFolder::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    Locker locker(SystemRegistrar::the().m_lock);
    VERIFY(m_parent_folder);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, m_parent_folder->component_index() }, 0 });

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    return KSuccess;
}

RefPtr<SystemExposedComponent> SystemExposedFolder::lookup(StringView name)
{
    for (auto& component : m_components) {
        if (component.name() == name) {
            return component;
        }
    }
    return {};
}

SystemExposedFolder::SystemExposedFolder(String name)
    : SystemExposedComponent(name)
{
}

SystemExposedFolder::SystemExposedFolder(String name, const SystemExposedFolder& parent_folder)
    : SystemExposedComponent(name)
    , m_parent_folder(parent_folder)
{
}

NonnullRefPtr<Inode> SystemExposedFolder::to_inode(const SysFS& sysfs_instance) const
{
    return SysFSDirectoryInode::create(sysfs_instance, *this);
}

NonnullRefPtr<Inode> SystemExposedComponent::to_inode(const SysFS& sysfs_instance) const
{
    return SysFSInode::create(sysfs_instance, *this);
}

}
