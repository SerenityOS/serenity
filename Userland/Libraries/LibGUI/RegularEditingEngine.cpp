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

#include <AK/QuickSort.h>
#include <LibGUI/RegularEditingEngine.h>
#include <LibGUI/TextEditor.h>

namespace GUI {

CursorWidth RegularEditingEngine::cursor_width() const
{
    return CursorWidth::NARROW;
}

bool RegularEditingEngine::on_key(const KeyEvent& event)
{
    if (EditingEngine::on_key(event))
        return true;

    if (event.key() == KeyCode::Key_Escape) {
        if (m_editor->on_escape_pressed)
            m_editor->on_escape_pressed();
        return true;
    }

    if (event.alt() && event.shift() && event.key() == KeyCode::Key_S) {
        sort_selected_lines();
        return true;
    }

    return false;
}

static int strcmp_utf32(const u32* s1, const u32* s2, size_t n)
{
    while (n-- > 0) {
        if (*s1++ != *s2++)
            return s1[-1] < s2[-1] ? -1 : 1;
    }
    return 0;
}

void RegularEditingEngine::sort_selected_lines()
{
    if (!m_editor->is_editable())
        return;

    if (!m_editor->has_selection())
        return;

    size_t first_line;
    size_t last_line;
    get_selection_line_boundaries(first_line, last_line);

    auto& lines = m_editor->document().lines();

    auto start = lines.begin() + (int)first_line;
    auto end = lines.begin() + (int)last_line + 1;

    quick_sort(start, end, [](auto& a, auto& b) {
        return strcmp_utf32(a.code_points(), b.code_points(), min(a.length(), b.length())) < 0;
    });

    m_editor->did_change();
    m_editor->update();
}

}
