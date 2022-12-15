/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/File.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class MountFile final : public File {
public:
    struct FileSystemInitializer {
        StringView short_name;
        StringView name;
        bool requires_open_file_description { false };
        bool requires_block_device { false };
        bool requires_seekable_file { false };
        ErrorOr<NonnullLockRefPtr<FileSystem>> (*create_with_fd)(OpenFileDescription&, Span<u8 const>) = nullptr;
        ErrorOr<NonnullLockRefPtr<FileSystem>> (*create)(Span<u8 const>) = nullptr;
        ErrorOr<void> (*handle_mount_boolean_flag)(Span<u8>, StringView key) = nullptr;
        ErrorOr<void> (*handle_mount_unsigned_integer_flag)(Span<u8>, StringView key, u64) = nullptr;
        ErrorOr<void> (*handle_mount_signed_integer_flag)(Span<u8>, StringView key, i64) = nullptr;
        ErrorOr<void> (*handle_mount_ascii_string_flag)(Span<u8>, StringView key, StringView value) = nullptr;
    };

    static ErrorOr<NonnullLockRefPtr<MountFile>> create(NonnullOwnPtr<KString> filesystem_type, int flags);
    virtual ~MountFile() override;

    virtual bool can_read(OpenFileDescription const&, u64) const override { return true; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const&) const override;
    virtual StringView class_name() const override { return "MountFile"sv; }

    MutexProtected<RefPtr<FileSystem>>& prepared_filesystem() { return m_filesystem; }
    int mount_flags() const { return m_flags; }

private:
    virtual bool is_mount_file() const override { return true; }

    MountFile(NonnullOwnPtr<KString> filesystem_type, int flags, NonnullOwnPtr<KBuffer>);

    int const m_flags;
    int m_source_fd { -1 };
    FileSystemInitializer const& m_filesystem_initializer;
    NonnullOwnPtr<KString> m_filesystem_type;
    NonnullOwnPtr<KBuffer> m_mount_specific_data;
    MutexProtected<RefPtr<FileSystem>> m_filesystem;
};

}
