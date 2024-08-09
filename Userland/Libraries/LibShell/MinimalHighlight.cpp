/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibShell/Highlight.h>

namespace Shell {

void Shell::print_path(StringView path)
{
    out("{}", path);
}

Optional<Line::Style> highlight_runnable(Shell&, Shell::RunnablePath&)
{
    return {};
}

ErrorOr<void> highlight_filesystem_path(StringView, Line::Editor&, Shell&, size_t, size_t)
{
    return {};
}

ErrorOr<void> highlight_filesystem_path_without_resolving(StringView, Line::Editor&, Shell&, size_t, size_t)
{
    return {};
}

}
