/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibLine/Editor.h>
#include <LibLine/Style.h>
#include <LibShell/Shell.h>

namespace Shell {

void print_path(StringView path);
Optional<Line::Style> highlight_runnable(Shell& shell, Shell::RunnablePath& runnable);
ErrorOr<void> highlight_filesystem_path(StringView path, Line::Editor& editor, Shell& shell, size_t start_offset, size_t end_offset);
ErrorOr<void> highlight_filesystem_path_without_resolving(StringView realpath, Line::Editor& editor, Shell& shell, size_t start_offset, size_t end_offset);

}
