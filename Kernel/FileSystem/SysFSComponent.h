/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Forward.h>

namespace Kernel {

struct SysFSInodeData : public OpenFileDescriptionData {
    OwnPtr<KBuffer> buffer;
};

class SysFSComponent : public RefCounted<SysFSComponent> {
public:
    virtual StringView name() const = 0;
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const { return Error::from_errno(ENOTIMPL); }
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const { VERIFY_NOT_REACHED(); }
    virtual RefPtr<SysFSComponent> lookup(StringView) { VERIFY_NOT_REACHED(); };
    virtual mode_t permissions() const;
    virtual ErrorOr<void> truncate(u64) { return EPERM; }
    virtual ErrorOr<void> set_mtime(time_t) { return ENOTIMPL; }
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) { return EROFS; }
    virtual ErrorOr<void> refresh_data(OpenFileDescription&) const { return {}; }

    virtual ErrorOr<NonnullRefPtr<SysFSInode>> to_inode(SysFS const&) const;

    InodeIndex component_index() const { return m_component_index; };

    virtual ~SysFSComponent() = default;

protected:
    SysFSComponent();

private:
    InodeIndex m_component_index {};
};

class SysFSDirectory : public SysFSComponent {
public:
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

    virtual ErrorOr<NonnullRefPtr<SysFSInode>> to_inode(SysFS const& sysfs_instance) const override final;

protected:
    SysFSDirectory() = default;
    explicit SysFSDirectory(SysFSDirectory const& parent_directory);
    NonnullRefPtrVector<SysFSComponent> m_components;
    RefPtr<SysFSDirectory> m_parent_directory;
};

}
