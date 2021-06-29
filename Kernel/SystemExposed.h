/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/KResult.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

class SysFS;
class SystemExposedComponent : public RefCounted<SystemExposedComponent> {
public:
    virtual KResultOr<size_t> entries_count() const { VERIFY_NOT_REACHED(); };
    virtual StringView name() const { return m_name->view(); }
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, FileDescription*) const { VERIFY_NOT_REACHED(); }
    virtual KResult traverse_as_directory(unsigned, Function<bool(const FS::DirectoryEntryView&)>) const { VERIFY_NOT_REACHED(); }
    virtual RefPtr<SystemExposedComponent> lookup(StringView) { VERIFY_NOT_REACHED(); };
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer&, FileDescription*) { return -EROFS; }
    virtual size_t size() const { return 0; }

    virtual NonnullRefPtr<Inode> to_inode(const SysFS& sysfs_instance) const;

    size_t component_index() const { return m_component_index; };

    virtual ~SystemExposedComponent() = default;

protected:
    explicit SystemExposedComponent(StringView name);

private:
    NonnullOwnPtr<KString> m_name;
    size_t m_component_index;
};

class SystemExposedFolder : public SystemExposedComponent {
public:
    virtual KResultOr<size_t> entries_count() const override { return m_components.size(); };
    virtual KResult traverse_as_directory(unsigned, Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<SystemExposedComponent> lookup(StringView name) override;
    void add_component(const SystemExposedComponent&);

    virtual NonnullRefPtr<Inode> to_inode(const SysFS& sysfs_instance) const override final;

protected:
    explicit SystemExposedFolder(String name);
    SystemExposedFolder(String name, const SystemExposedFolder& parent_folder);
    NonnullRefPtrVector<SystemExposedComponent> m_components;
    RefPtr<SystemExposedFolder> m_parent_folder;
};

}
