/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/File.h>
#include <Kernel/Memory/AnonymousVMObject.h>

namespace Kernel {

class AnonymousFile final : public File {
public:
    static KResultOr<NonnullRefPtr<AnonymousFile>> try_create(NonnullRefPtr<Memory::AnonymousVMObject> vmobject)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) AnonymousFile(move(vmobject)));
    }

    virtual ~AnonymousFile() override;

    virtual KResultOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;

private:
    virtual StringView class_name() const override { return "AnonymousFile"sv; }
    virtual KResultOr<NonnullOwnPtr<KString>> pseudo_path(const OpenFileDescription&) const;
    virtual bool can_read(const OpenFileDescription&, size_t) const override { return false; }
    virtual bool can_write(const OpenFileDescription&, size_t) const override { return false; }
    virtual KResultOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual KResultOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return ENOTSUP; }

    explicit AnonymousFile(NonnullRefPtr<Memory::AnonymousVMObject>);

    NonnullRefPtr<Memory::AnonymousVMObject> m_vmobject;
};

}
