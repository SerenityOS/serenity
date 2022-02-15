/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CellSyntaxHighlighter.h"
#include <LibGUI/TextEditor.h>
#include <LibGfx/Palette.h>
#include <LibJS/Lexer.h>

namespace Spreadsheet {

void CellSyntaxHighlighter::rehighlight(const Palette& palette)
{
    auto text = m_client->get_text();
    m_client->spans().clear();
    if (!text.starts_with('=')) {
        m_client->do_update();
        return;
    }

    JS::SyntaxHighlighter::rehighlight(palette);

    // Highlight the '='
    m_client->spans().empend(
        GUI::TextRange { { 0, 0 }, { 0, 1 } },
        Gfx::TextAttributes {
            palette.syntax_keyword(),
            Optional<Color> {},
            false,
            false,
        },
        (u64)-1,
        false);

    if (m_cell && m_cell->thrown_value().has_value()) {
        if (auto value = m_cell->thrown_value().value(); value.is_object() && is<JS::Error>(value.as_object())) {
            auto& error = static_cast<JS::Error const&>(value.as_object());
            auto& traceback = error.traceback();
            auto& range = traceback.first().source_range;
            GUI::TextRange text_range { { range.start.line - 1, range.start.column }, { range.end.line - 1, range.end.column } };
            m_client->spans().prepend(
                GUI::TextDocumentSpan {
                    text_range,
                    Gfx::TextAttributes {
                        Color::Black,
                        Color::Red,
                        false,
                        false,
                    },
                    (u64)-1,
                    false });
        }
    }
    m_client->do_update();
}
}
