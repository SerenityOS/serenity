/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2023, Matteo benetti <matteo.benetti@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CellSyntaxHighlighter.h"
#include <LibGUI/TextEditor.h>
#include <LibGfx/Palette.h>
#include <LibJS/Lexer.h>

namespace Spreadsheet {

void CellSyntaxHighlighter::rehighlight(Palette const& palette)
{
    m_client->clear_spans();

    if (m_client->get_text().starts_with('=')) {

        JS::SyntaxHighlighter::rehighlight(palette);

        auto spans = m_client->spans();

        // Highlight the '='
        spans.empend(
            GUI::TextRange { { 0, 0 }, { 0, 1 } },
            Gfx::TextAttributes {
                palette.syntax_keyword(),
                Optional<Color> {},
                false,
            },
            (u64)-1,
            false);

        if (m_cell && m_cell->thrown_value().has_value()) {
            if (auto value = m_cell->thrown_value().value(); value.is_object() && is<JS::Error>(value.as_object())) {
                auto& error = static_cast<JS::Error const&>(value.as_object());
                auto& range = error.traceback().first().source_range();

                spans.prepend({
                    GUI::TextRange { { range.start.line - 1, range.start.column }, { range.end.line - 1, range.end.column } },
                    Gfx::TextAttributes {
                        Color::Black,
                        Color::Red,
                        false,
                    },
                    (u64)-1,
                    false,
                });
            }
        }

        m_client->do_set_spans(move(spans));
    }

    m_client->do_update();
}
}
