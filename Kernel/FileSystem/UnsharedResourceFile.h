/*
 * Copyright (c) 2026, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/Unshare.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class UnsharedResourceFile final : public File {
public:
    static ErrorOr<NonnullRefPtr<UnsharedResourceFile>> create(UnshareType);
    virtual ~UnsharedResourceFile() override;

    virtual bool can_read(OpenFileDescription const&, u64) const override { return true; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const&) const override;
    virtual StringView class_name() const override { return "UnsharedResourceFile"sv; }

    ErrorOr<unsigned> initialize_resource();

private:
    virtual bool is_unshared_resource_file() const override { return true; }

    explicit UnsharedResourceFile(UnshareType);
    UnshareType const m_type;

    MutexProtected<RefPtr<FileSystem>> m_root_filesystem;
};

}
