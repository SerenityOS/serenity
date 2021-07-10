/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/KResult.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

class SysFS;
class SysFSComponent : public RefCounted<SysFSComponent> {
public:
    virtual KResultOr<size_t> entries_count() const { VERIFY_NOT_REACHED(); };
    virtual StringView name() const { return m_name->view(); }
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, FileDescription*) const { VERIFY_NOT_REACHED(); }
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const { VERIFY_NOT_REACHED(); }
    virtual RefPtr<SysFSComponent> lookup(StringView) { VERIFY_NOT_REACHED(); };
    virtual KResultOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, FileDescription*) { return -EROFS; }
    virtual size_t size() const { return 0; }

    virtual NonnullRefPtr<Inode> to_inode(SysFS const&) const;

    InodeIndex component_index() const { return m_component_index; };

    virtual ~SysFSComponent() = default;

protected:
    explicit SysFSComponent(StringView name);

private:
    NonnullOwnPtr<KString> m_name;
    InodeIndex m_component_index {};
};

class SystemExposedFolder : public SysFSComponent {
public:
    virtual KResultOr<size_t> entries_count() const override { return m_components.size(); };
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;
    void add_component(SysFSComponent const&);

    virtual NonnullRefPtr<Inode> to_inode(SysFS const& sysfs_instance) const override final;

protected:
    explicit SystemExposedFolder(StringView name);
    SystemExposedFolder(StringView name, SystemExposedFolder const& parent_folder);
    NonnullRefPtrVector<SysFSComponent> m_components;
    RefPtr<SystemExposedFolder> m_parent_folder;
};

}
