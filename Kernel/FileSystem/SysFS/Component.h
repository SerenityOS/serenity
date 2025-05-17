/*
 * Copyright (c) 2021-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/RAMBackedFileType.h>
#include <Kernel/Forward.h>

namespace Kernel {

struct SysFSInodeData : public OpenFileDescriptionData {
    OwnPtr<KBuffer> buffer;
};

class SysFSDirectory;
class SysFSComponent : public AtomicRefCounted<SysFSComponent> {
    friend class SysFSDirectory;

public:
    // NOTE: It is safe to assume that the regular file type is largely
    // the most used file type in the SysFS filesystem.
    virtual RAMBackedFileType type() const { return RAMBackedFileType::Regular; }

    virtual StringView name() const = 0;
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const { return Error::from_errno(ENOTIMPL); }
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const { VERIFY_NOT_REACHED(); }
    virtual RefPtr<SysFSComponent> lookup(StringView) { VERIFY_NOT_REACHED(); }
    virtual mode_t permissions() const;
    virtual ErrorOr<void> truncate(u64) { return EPERM; }
    virtual size_t size() const { return 0; }
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) { return EROFS; }
    virtual ErrorOr<void> refresh_data(OpenFileDescription&) const { return {}; }

    virtual ErrorOr<NonnullRefPtr<SysFSInode>> to_inode(SysFS const&) const;

    InodeIndex component_index() const { return m_component_index; }

    virtual ~SysFSComponent() = default;

    ErrorOr<NonnullOwnPtr<KString>> relative_path(NonnullOwnPtr<KString>, size_t current_hop = 0) const;
    ErrorOr<size_t> relative_path_hops_count_from_mountpoint(size_t current_hop = 0) const;

protected:
    explicit SysFSComponent(SysFSDirectory const& parent_directory);
    SysFSComponent();

    RefPtr<SysFSDirectory> const m_parent_directory;

    IntrusiveListNode<SysFSComponent, NonnullRefPtr<SysFSComponent>> m_list_node;

private:
    InodeIndex m_component_index {};
};

class SysFSSymbolicLink : public SysFSComponent {
public:
    virtual RAMBackedFileType type() const override final { return RAMBackedFileType::Link; }
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullRefPtr<SysFSInode>> to_inode(SysFS const& sysfs_instance) const override final;

protected:
    ErrorOr<NonnullOwnPtr<KString>> try_generate_return_path_to_mount_point() const;
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;

    explicit SysFSSymbolicLink(SysFSDirectory const& parent_directory, SysFSComponent const& pointed_component);

    NonnullRefPtr<SysFSComponent> const m_pointed_component;
};

class SysFSDirectory : public SysFSComponent {
public:
    virtual RAMBackedFileType type() const override final { return RAMBackedFileType::Directory; }
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override final;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override final;

    virtual ErrorOr<NonnullRefPtr<SysFSInode>> to_inode(SysFS const& sysfs_instance) const override final;

    using ChildList = SpinlockProtected<IntrusiveList<&SysFSComponent::m_list_node>, LockRank::None>;

protected:
    virtual bool is_root_directory() const { return false; }

    SysFSDirectory() {};
    explicit SysFSDirectory(SysFSDirectory const& parent_directory);
    ChildList m_child_components {};
};

}
