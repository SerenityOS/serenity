/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileSystemSpecificOption.h>
#include <Kernel/FileSystem/Initializer.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class MountFile final : public File {
public:
    static ErrorOr<NonnullLockRefPtr<MountFile>> create(FileSystemInitializer const&, int flags);
    virtual ~MountFile() override;

    virtual bool can_read(OpenFileDescription const&, u64) const override { return true; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const&) const override;
    virtual StringView class_name() const override { return "MountFile"sv; }

    int mount_flags() const { return m_flags; }

    MutexProtected<FileSystemSpecificOptions>& filesystem_specific_options() { return m_filesystem_specific_options; }
    FileSystemInitializer const& file_system_initializer() const { return m_file_system_initializer; }

private:
    virtual bool is_mount_file() const override { return true; }

    MountFile(FileSystemInitializer const&, int flags);

    int const m_flags;
    FileSystemInitializer const& m_file_system_initializer;
    MutexProtected<FileSystemSpecificOptions> m_filesystem_specific_options;
};

}
