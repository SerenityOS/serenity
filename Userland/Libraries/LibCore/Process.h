/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Span.h>

namespace Core {

class Process {
public:
    static ErrorOr<pid_t> spawn(StringView path, Span<DeprecatedString const> arguments, DeprecatedString working_directory = {});
    static ErrorOr<pid_t> spawn(StringView path, Span<StringView const> arguments, DeprecatedString working_directory = {});
    static ErrorOr<pid_t> spawn(StringView path, Span<char const* const> arguments = {}, DeprecatedString working_directory = {});

    static ErrorOr<String> get_name();
    enum class SetThreadName {
        No,
        Yes,
    };
    static ErrorOr<void> set_name(StringView, SetThreadName = SetThreadName::No);
};

}
