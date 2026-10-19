/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileSystemSpecificOption.h>

namespace Kernel {

template<OneOf<u64, i64, bool> T>
static Optional<T> parse_impl(FileSystemSpecificOptions const& filesystem_specific_options, StringView name)
{
    Optional<T> maybe_value;
    auto maybe_flag = filesystem_specific_options.get(name);
    if (maybe_flag.has_value()) {
        maybe_flag.value()->property_value().visit(
            [&](T value) {
                maybe_value = value;
            },
            [&](auto) {
                VERIFY_NOT_REACHED();
            });
    }
    return maybe_value;
}

Optional<u64> parse_unsigned_filesystem_specific_option(FileSystemSpecificOptions const& filesystem_specific_options, StringView name)
{
    return parse_impl<u64>(filesystem_specific_options, name);
}

Optional<i64> parse_signed_filesystem_specific_option(FileSystemSpecificOptions const& filesystem_specific_options, StringView name)
{
    return parse_impl<i64>(filesystem_specific_options, name);
}

Optional<bool> parse_bool_filesystem_specific_option(FileSystemSpecificOptions const& filesystem_specific_options, StringView name)
{
    return parse_impl<bool>(filesystem_specific_options, name);
}

ErrorOr<NonnullOwnPtr<FileSystemSpecificOption>> FileSystemSpecificOption::create_as_unsigned(u64 value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) FileSystemSpecificOption(value));
}

ErrorOr<NonnullOwnPtr<FileSystemSpecificOption>> FileSystemSpecificOption::create_as_signed(i64 value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) FileSystemSpecificOption(value));
}

ErrorOr<NonnullOwnPtr<FileSystemSpecificOption>> FileSystemSpecificOption::create_as_boolean(bool value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) FileSystemSpecificOption(value));
}

FileSystemSpecificOption::FileSystemSpecificOption(u64 value)
    : m_value(value)
{
}

FileSystemSpecificOption::FileSystemSpecificOption(i64 value)
    : m_value(value)
{
}

FileSystemSpecificOption::FileSystemSpecificOption(bool value)
    : m_value(value)
{
}

}
