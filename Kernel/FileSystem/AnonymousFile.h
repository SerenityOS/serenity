/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/File.h>

namespace Kernel {

class AnonymousFile final : public File {
public:
    static RefPtr<AnonymousFile> create(NonnullRefPtr<AnonymousVMObject> vmobject)
    {
        return adopt_ref_if_nonnull(new AnonymousFile(move(vmobject)));
    }

    virtual ~AnonymousFile() override;

    virtual KResultOr<Region*> mmap(Process&, FileDescription&, const Range&, u64 offset, int prot, bool shared) override;

private:
    virtual const char* class_name() const override { return "AnonymousFile"; }
    virtual String absolute_path(const FileDescription&) const override { return ":anonymous-file:"; }
    virtual bool can_read(const FileDescription&, size_t) const override { return false; }
    virtual bool can_write(const FileDescription&, size_t) const override { return false; }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return ENOTSUP; }

    explicit AnonymousFile(NonnullRefPtr<AnonymousVMObject>);

    NonnullRefPtr<AnonymousVMObject> m_vmobject;
};

}
