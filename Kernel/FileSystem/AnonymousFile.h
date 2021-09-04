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
    static RefPtr<AnonymousFile> create(NonnullRefPtr<Memory::AnonymousVMObject> vmobject)
    {
        return adopt_ref_if_nonnull(new (nothrow) AnonymousFile(move(vmobject)));
    }

    virtual ~AnonymousFile() override;

    virtual KResultOr<Memory::Region*> mmap(Process&, FileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;

private:
    virtual StringView class_name() const override { return "AnonymousFile"; }
    virtual String absolute_path(FileDescription const&) const override { return ":anonymous-file:"; }
    virtual bool can_read(FileDescription const&, size_t) const override { return false; }
    virtual bool can_write(FileDescription const&, size_t) const override { return false; }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual KResultOr<size_t> write(FileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return ENOTSUP; }

    explicit AnonymousFile(NonnullRefPtr<Memory::AnonymousVMObject>);

    NonnullRefPtr<Memory::AnonymousVMObject> m_vmobject;
};

}
