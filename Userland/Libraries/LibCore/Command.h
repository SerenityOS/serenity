/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <spawn.h>

namespace Core {

// If the executed command fails, the returned String will be in the null state.

struct CommandResult {
    int exit_code { 0 };
    DeprecatedString output;
    DeprecatedString error;
};

ErrorOr<CommandResult> command(DeprecatedString const& program, Vector<DeprecatedString> const& arguments, Optional<LexicalPath> chdir);
ErrorOr<CommandResult> command(DeprecatedString const& command_string, Optional<LexicalPath> chdir);

}
