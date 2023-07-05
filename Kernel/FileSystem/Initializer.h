/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>

namespace Kernel {

struct FileSystemInitializer {
    StringView short_name;
    StringView name;
    bool requires_open_file_description { false };
    bool requires_block_device { false };
    bool requires_seekable_file { false };
    ErrorOr<NonnullRefPtr<FileSystem>> (*create_with_fd)(OpenFileDescription&, ReadonlyBytes) = nullptr;
    ErrorOr<NonnullRefPtr<FileSystem>> (*create)(ReadonlyBytes) = nullptr;
    ErrorOr<void> (*handle_mount_boolean_flag)(Span<u8>, StringView key, bool) = nullptr;
    ErrorOr<void> (*handle_mount_unsigned_integer_flag)(Span<u8>, StringView key, u64) = nullptr;
    ErrorOr<void> (*handle_mount_signed_integer_flag)(Span<u8>, StringView key, i64) = nullptr;
    ErrorOr<void> (*handle_mount_ascii_string_flag)(Span<u8>, StringView key, StringView value) = nullptr;
};

}
