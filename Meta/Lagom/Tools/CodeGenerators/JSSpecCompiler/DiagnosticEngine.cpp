/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DiagnosticEngine.h"

namespace JSSpecCompiler {

bool DiagnosticEngine::has_fatal_errors() const
{
    return m_has_fatal_errors;
}

void DiagnosticEngine::print_diagnostics()
{
    auto use_color = isatty(STDERR_FILENO) ? UseColor::Yes : UseColor::No;

    StringBuilder builder;
    for (auto const& diagnostic : m_diagnostics)
        diagnostic.format_into(builder, use_color);

    out(stderr, "{}", builder.string_view());
}

void DiagnosticEngine::Diagnostic::format_into(StringBuilder& builder, UseColor use_color) const
{
    if (!location.filename.is_empty())
        builder.appendff("{}:{}:{}: ", location.filename, location.line + 1, location.column + 1);

    static constexpr Array<StringView, 4> colored_diagnostic_levels = { {
        "\e[1mnote\e[0m"sv,
        "\e[1;33mwarning\e[0m"sv,
        "\e[1;31merror\e[0m"sv,
        "\e[1;31mfatal error\e[0m"sv,
    } };
    static constexpr Array<StringView, 4> diagnostic_levels = { {
        "note"sv,
        "warning"sv,
        "error"sv,
        "fatal error"sv,
    } };

    auto diagnostic_level_text = (use_color == UseColor::Yes ? colored_diagnostic_levels : diagnostic_levels);
    builder.appendff("{}: ", diagnostic_level_text[to_underlying(level)]);

    if (auto logical_location = location.logical_location) {
        if (!logical_location->section.is_empty()) {
            builder.appendff("in {}", logical_location->section);
            if (!logical_location->step.is_empty())
                builder.appendff(" step {}", logical_location->step);
            builder.appendff(": ");
        }
    }

    builder.append(message);
    builder.append('\n');

    for (auto const& note : notes)
        note.format_into(builder, use_color);
}

void DiagnosticEngine::add_diagnostic(Diagnostic&& diagnostic)
{
    if (diagnostic.level == DiagnosticLevel::FatalError)
        m_has_fatal_errors = true;
    if (diagnostic.level != DiagnosticLevel::Note)
        m_diagnostics.append(move(diagnostic));
    else
        m_diagnostics.last().notes.append(move(diagnostic));
}

}
