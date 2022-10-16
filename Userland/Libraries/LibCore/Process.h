/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Span.h>

namespace Core {

class Process {
public:
    static ErrorOr<pid_t> spawn(StringView path, Span<String const> arguments, String working_directory = {});
    static ErrorOr<pid_t> spawn(StringView path, Span<StringView const> arguments, String working_directory = {});
    static ErrorOr<pid_t> spawn(StringView path, Span<char const* const> arguments = {}, String working_directory = {});
};

}
