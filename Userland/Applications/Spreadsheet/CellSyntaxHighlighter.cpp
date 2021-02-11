/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        nullptr,
        false);

    if (m_cell && m_cell->exception()) {
        auto range = m_cell->exception()->source_ranges().first();
        GUI::TextRange text_range { { range.start.line - 1, range.start.column }, { range.end.line - 1, range.end.column - 1 } };
        m_client->spans().prepend(
            GUI::TextDocumentSpan {
                text_range,
                Gfx::TextAttributes {
                    Color::Black,
                    Color::Red,
                    false,
                    false,
                },
                nullptr,
                false });
    }
    m_client->do_update();
}

CellSyntaxHighlighter::~CellSyntaxHighlighter()
{
}

}
