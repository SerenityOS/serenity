/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/FileSystem/SysFSComponent.h>

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

SysFSComponent::SysFSComponent(StringView name)
    : m_name(KString::try_create(name).release_value()) // FIXME: Handle KString allocation failure.
    , m_component_index(allocate_inode_index())
{
}

KResult SysFSDirectory::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(SysFSComponentRegistry::the().get_lock());
    VERIFY(m_parent_directory);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, m_parent_directory->component_index() }, 0 });

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    return KSuccess;
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

SysFSDirectory::SysFSDirectory(StringView name)
    : SysFSComponent(name)
{
}

SysFSDirectory::SysFSDirectory(StringView name, SysFSDirectory const& parent_directory)
    : SysFSComponent(name)
    , m_parent_directory(parent_directory)
{
}

KResultOr<NonnullRefPtr<SysFSInode>> SysFSDirectory::to_inode(SysFS const& sysfs_instance) const
{
    return TRY(SysFSDirectoryInode::try_create(sysfs_instance, *this));
}

KResultOr<NonnullRefPtr<SysFSInode>> SysFSComponent::to_inode(SysFS const& sysfs_instance) const
{
    return SysFSInode::try_create(sysfs_instance, *this);
}

}
