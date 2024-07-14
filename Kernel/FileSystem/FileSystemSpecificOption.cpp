/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileSystemSpecificOption.h>

namespace Kernel {

Optional<u64> parse_unsigned_filesystem_specific_option(FileSystemSpecificOptions const& filesystem_specific_options, StringView name)
{
    Optional<u64> maybe_value;
    auto maybe_flag = filesystem_specific_options.get(name);
    if (maybe_flag.has_value()) {
        maybe_flag.value()->property_value().visit(
            [&](unsigned value) {
                maybe_value = value;
            },
            [&](NonnullOwnPtr<KString>) {
                VERIFY_NOT_REACHED();
            },
            [&](bool) {
                VERIFY_NOT_REACHED();
            },
            [&](signed) {
                VERIFY_NOT_REACHED();
            });
    }
    return maybe_value;
}

Optional<i64> parse_signed_filesystem_specific_option(FileSystemSpecificOptions const& filesystem_specific_options, StringView name)
{
    Optional<i64> maybe_value;
    auto maybe_flag = filesystem_specific_options.get(name);
    if (maybe_flag.has_value()) {
        maybe_flag.value()->property_value().visit(
            [&](unsigned) {
                VERIFY_NOT_REACHED();
            },
            [&](NonnullOwnPtr<KString>) {
                VERIFY_NOT_REACHED();
            },
            [&](bool) {
                VERIFY_NOT_REACHED();
            },
            [&](signed value) {
                maybe_value = value;
            });
    }
    return maybe_value;
}

Optional<bool> parse_bool_filesystem_specific_option(FileSystemSpecificOptions const& filesystem_specific_options, StringView name)
{
    Optional<bool> maybe_value;
    auto maybe_flag = filesystem_specific_options.get(name);
    if (maybe_flag.has_value()) {
        maybe_flag.value()->property_value().visit(
            [&](unsigned) {
                VERIFY_NOT_REACHED();
            },
            [&](NonnullOwnPtr<KString>) {
                VERIFY_NOT_REACHED();
            },
            [&](bool value) {
                maybe_value = value;
            },
            [&](signed) {
                VERIFY_NOT_REACHED();
            });
    }
    return maybe_value;
}

ErrorOr<NonnullOwnPtr<FileSystemSpecificOption>> FileSystemSpecificOption::create_as_unsigned(unsigned value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) FileSystemSpecificOption(value));
}

ErrorOr<NonnullOwnPtr<FileSystemSpecificOption>> FileSystemSpecificOption::create_as_signed(signed value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) FileSystemSpecificOption(value));
}

ErrorOr<NonnullOwnPtr<FileSystemSpecificOption>> FileSystemSpecificOption::create_as_boolean(bool value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) FileSystemSpecificOption(value));
}

FileSystemSpecificOption::FileSystemSpecificOption(unsigned value)
    : m_value(value)
{
}

FileSystemSpecificOption::FileSystemSpecificOption(signed value)
    : m_value(value)
{
}

FileSystemSpecificOption::FileSystemSpecificOption(bool value)
    : m_value(value)
{
}

}
