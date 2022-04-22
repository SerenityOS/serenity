/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/KLexicalPath.h>

namespace Kernel {

static Spinlock s_index_lock;
static InodeIndex s_next_inode_index { 0 };

static size_t allocate_inode_index()
{
    SpinlockLocker lock(s_index_lock);
    s_next_inode_index = s_next_inode_index.value() + 1;
    VERIFY(s_next_inode_index > 0);
    return s_next_inode_index.value();
}

SysFSComponent::SysFSComponent(SysFSDirectory const& parent_directory)
    : m_parent_directory(parent_directory)
    , m_component_index(allocate_inode_index())
{
}

SysFSComponent::SysFSComponent()
    : m_component_index(allocate_inode_index())
{
}

ErrorOr<NonnullOwnPtr<KString>> SysFSComponent::relative_path(NonnullOwnPtr<KString> name, size_t current_hop) const
{
    if (current_hop >= 128)
        return Error::from_errno(ELOOP);
    if (!m_parent_directory)
        return name;
    auto joined_name = TRY(KLexicalPath::try_join(m_parent_directory->name(), name->view()));
    return m_parent_directory->relative_path(move(joined_name), current_hop + 1);
}

ErrorOr<size_t> SysFSComponent::relative_path_hops_count_from_mountpoint(size_t current_hop) const
{
    if (current_hop >= 128)
        return Error::from_errno(ELOOP);
    if (!m_parent_directory)
        return current_hop;
    return m_parent_directory->relative_path_hops_count_from_mountpoint(current_hop + 1);
}

mode_t SysFSComponent::permissions() const
{
    return S_IRUSR | S_IRGRP | S_IROTH;
}

ErrorOr<void> SysFSDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(SysFSComponentRegistry::the().get_lock());
    VERIFY(m_parent_directory);
    TRY(callback({ "."sv, { fsid, component_index() }, 0 }));
    TRY(callback({ ".."sv, { fsid, m_parent_directory->component_index() }, 0 }));

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        TRY(callback({ component.name(), identifier, 0 }));
    }
    return {};
}

RefPtr<SysFSComponent> SysFSDirectory::lookup(StringView name)
{
    for (auto& component : m_components) {
        if (component.name() == name) {
            return component;
        }
    }
    return {};
}

SysFSDirectory::SysFSDirectory(SysFSDirectory const& parent_directory)
    : SysFSComponent(parent_directory)
{
}

ErrorOr<NonnullRefPtr<SysFSInode>> SysFSDirectory::to_inode(SysFS const& sysfs_instance) const
{
    return TRY(SysFSDirectoryInode::try_create(sysfs_instance, *this));
}

ErrorOr<NonnullRefPtr<SysFSInode>> SysFSComponent::to_inode(SysFS const& sysfs_instance) const
{
    return SysFSInode::try_create(sysfs_instance, *this);
}

}
