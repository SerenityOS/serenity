/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibShell/Highlight.h>
#include <LibURL/URL.h>
#include <unistd.h>

namespace Shell {

void Shell::print_path(StringView path)
{
    if (!m_is_interactive || !isatty(STDOUT_FILENO)) {
        out("{}", path);
        return;
    }
    auto url = URL::create_with_file_scheme(path, {}, hostname);
    out("\033]8;;{}\033\\{}\033]8;;\033\\", url.serialize(), path);
}

Optional<Line::Style> highlight_runnable(Shell& shell, Shell::RunnablePath& runnable)
{
    Line::Style bold = { Line::Style::Bold };
    VERIFY(runnable.kind == Shell::RunnablePath::Kind::Executable || runnable.kind == Shell::RunnablePath::Kind::Alias);
    auto name = shell.help_path_for({}, runnable);
    if (name.has_value()) {
        auto url = URL::create_with_help_scheme(name.release_value(), shell.hostname);
        return bold.unified_with(Line::Style::Hyperlink(url.to_byte_string()));
    }
    return {};
}

ErrorOr<void> highlight_filesystem_path(StringView path, Line::Editor& editor, Shell& shell, size_t start_offset, size_t end_offset)
{
    auto realpath = shell.resolve_path(path);
    return highlight_filesystem_path_without_resolving(path, editor, shell, start_offset, end_offset);
}

ErrorOr<void> highlight_filesystem_path_without_resolving(StringView realpath, Line::Editor& editor, Shell& shell, size_t start_offset, size_t end_offset)
{
    auto url = URL::create_with_file_scheme(realpath);
    url.set_host(TRY(String::from_byte_string(shell.hostname)));
    editor.stylize({ start_offset, end_offset }, { Line::Style::Hyperlink(url.to_byte_string()) });
    return {};
}

}
