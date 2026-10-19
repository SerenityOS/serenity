/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/FixedStringBuffer.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <AK/Variant.h>
#include <Kernel/API/FileSystem/MountSpecificFlags.h>
#include <Kernel/Library/KString.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class FileSystemSpecificOption {
public:
    static ErrorOr<NonnullOwnPtr<FileSystemSpecificOption>> create_as_unsigned(u64);
    static ErrorOr<NonnullOwnPtr<FileSystemSpecificOption>> create_as_signed(i64);
    static ErrorOr<NonnullOwnPtr<FileSystemSpecificOption>> create_as_boolean(bool);

    Variant<u64, i64, bool> const& property_value() const { return m_value; }

private:
    explicit FileSystemSpecificOption(u64);
    explicit FileSystemSpecificOption(i64);
    explicit FileSystemSpecificOption(bool);

    Variant<u64, i64, bool> const m_value;
};

using FileSystemSpecificOptions = HashMap<NonnullOwnPtr<KString>, NonnullOwnPtr<FileSystemSpecificOption>>;

Optional<u64> parse_unsigned_filesystem_specific_option(FileSystemSpecificOptions const&, StringView name);
Optional<i64> parse_signed_filesystem_specific_option(FileSystemSpecificOptions const&, StringView name);
Optional<bool> parse_bool_filesystem_specific_option(FileSystemSpecificOptions const&, StringView name);

}
