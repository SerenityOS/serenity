/*
 * Copyright (c) 2021-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/DirectoryInode.h>
#include <Kernel/FileSystem/SysFS/Inode.h>
#include <Kernel/FileSystem/SysFS/LinkInode.h>
#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/Library/KLexicalPath.h>

namespace Kernel {

static Spinlock<LockRank::None> s_index_lock {};
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

ErrorOr<size_t> SysFSSymbolicLink::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullOwnPtr<KBuffer>> SysFSSymbolicLink::try_to_generate_buffer() const
{
    auto return_path_to_mount_point = TRY(try_generate_return_path_to_mount_point());
    auto pointed_component_base_name = MUST(KString::try_create(m_pointed_component->name()));
    auto pointed_component_relative_path = MUST(m_pointed_component->relative_path(move(pointed_component_base_name), 0));
    auto full_return_and_target_path = TRY(KString::formatted("{}{}", return_path_to_mount_point->view(), pointed_component_relative_path->view()));
    return KBuffer::try_create_with_bytes("SysFSSymbolicLink"sv, full_return_and_target_path->view().bytes());
}

static ErrorOr<NonnullOwnPtr<KString>> generate_return_path_to_mount_point(NonnullOwnPtr<KString> current_path, size_t remaining_hop)
{
    if (remaining_hop == 0)
        return current_path;
    auto new_path = TRY(KString::formatted("../{}"sv, current_path->view()));
    remaining_hop--;
    return generate_return_path_to_mount_point(move(new_path), remaining_hop);
}

ErrorOr<NonnullOwnPtr<KString>> SysFSSymbolicLink::try_generate_return_path_to_mount_point() const
{
    VERIFY(m_parent_directory);
    auto hops_from_mountpoint = TRY(m_parent_directory->relative_path_hops_count_from_mountpoint());
    if (hops_from_mountpoint == 0)
        return KString::try_create("./"sv);
    auto start_return_path = TRY(KString::try_create("./"sv));
    return generate_return_path_to_mount_point(move(start_return_path), hops_from_mountpoint);
}

SysFSSymbolicLink::SysFSSymbolicLink(SysFSDirectory const& parent_directory, SysFSComponent const& pointed_component)
    : SysFSComponent(parent_directory)
    , m_pointed_component(pointed_component)
{
}

ErrorOr<void> SysFSDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    TRY(callback({ "."sv, { fsid, component_index() }, to_underlying(RAMBackedFileType::Directory) }));
    if (is_root_directory()) {
        TRY(callback({ ".."sv, { fsid, component_index() }, to_underlying(RAMBackedFileType::Directory) }));
    } else {
        VERIFY(m_parent_directory);
        TRY(callback({ ".."sv, { fsid, m_parent_directory->component_index() }, to_underlying(RAMBackedFileType::Directory) }));
    }

    return m_child_components.with([&](auto& list) -> ErrorOr<void> {
        for (auto& child_component : list) {
            InodeIdentifier identifier = { fsid, child_component.component_index() };
            TRY(callback({ child_component.name(), identifier, to_underlying(child_component.type()) }));
        }
        return {};
    });
}

RefPtr<SysFSComponent> SysFSDirectory::lookup(StringView name)
{
    return m_child_components.with([&](auto& list) -> RefPtr<SysFSComponent> {
        for (auto& child_component : list) {
            if (child_component.name() == name) {
                return child_component;
            }
        }
        return nullptr;
    });
}

SysFSDirectory::SysFSDirectory(SysFSDirectory const& parent_directory)
    : SysFSComponent(parent_directory)
{
}

ErrorOr<NonnullRefPtr<SysFSInode>> SysFSDirectory::to_inode(SysFS const& sysfs_instance) const
{
    return TRY(SysFSDirectoryInode::try_create(sysfs_instance, *this));
}

ErrorOr<NonnullRefPtr<SysFSInode>> SysFSSymbolicLink::to_inode(SysFS const& sysfs_instance) const
{
    return TRY(SysFSLinkInode::try_create(sysfs_instance, *this));
}

ErrorOr<NonnullRefPtr<SysFSInode>> SysFSComponent::to_inode(SysFS const& sysfs_instance) const
{
    return SysFSInode::try_create(sysfs_instance, *this);
}

}
