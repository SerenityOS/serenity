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
#include <Kernel/API/KResult.h>
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
    virtual StringView name() const { return m_name->view(); }
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const { VERIFY_NOT_REACHED(); }
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const { VERIFY_NOT_REACHED(); }
    virtual RefPtr<SysFSComponent> lookup(StringView) { VERIFY_NOT_REACHED(); };
    virtual KResultOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) { return EROFS; }
    virtual KResult refresh_data(OpenFileDescription&) const { return KSuccess; }

    virtual KResultOr<NonnullRefPtr<SysFSInode>> to_inode(SysFS const&) const;

    InodeIndex component_index() const { return m_component_index; };

    virtual ~SysFSComponent() = default;

protected:
    explicit SysFSComponent(StringView name);

private:
    NonnullOwnPtr<KString> m_name;
    InodeIndex m_component_index {};
};

class SysFSDirectory : public SysFSComponent {
public:
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

    virtual KResultOr<NonnullRefPtr<SysFSInode>> to_inode(SysFS const& sysfs_instance) const override final;

protected:
    explicit SysFSDirectory(StringView name);
    SysFSDirectory(StringView name, SysFSDirectory const& parent_directory);
    NonnullRefPtrVector<SysFSComponent> m_components;
    RefPtr<SysFSDirectory> m_parent_directory;
};

}
