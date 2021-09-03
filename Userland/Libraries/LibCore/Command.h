/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/LexicalPath.h>
#include <YAK/Optional.h>
#include <YAK/String.h>
#include <spawn.h>

namespace Core {

// If the executed command fails, the returned String will be in the null state.
String command(const String& program, const Vector<String>& arguments, Optional<LexicalPath> chdir);
String command(const String& command_string, Optional<LexicalPath> chdir);

}
