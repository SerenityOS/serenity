/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/File.h>
#include <Kernel/Memory/AnonymousVMObject.h>

namespace Kernel {

class AnonymousFile final : public File {
public:
    static ErrorOr<NonnullRefPtr<AnonymousFile>> try_create(NonnullLockRefPtr<Memory::AnonymousVMObject> vmobject)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) AnonymousFile(move(vmobject)));
    }

    virtual ~AnonymousFile() override;

    virtual ErrorOr<VMObjectAndMemoryType> vmobject_and_memory_type_for_mmap(Process&, Memory::VirtualRange const&, u64& offset, bool shared) override;

private:
    virtual StringView class_name() const override { return "AnonymousFile"sv; }
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const&) const override;
    virtual bool can_read(OpenFileDescription const&, u64) const override { return false; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return false; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return ENOTSUP; }

    explicit AnonymousFile(NonnullLockRefPtr<Memory::AnonymousVMObject>);

    NonnullLockRefPtr<Memory::AnonymousVMObject> m_vmobject;
};

}
