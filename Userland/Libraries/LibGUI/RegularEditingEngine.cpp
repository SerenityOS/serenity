/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibGUI/RegularEditingEngine.h>
#include <LibGUI/TextEditor.h>

namespace GUI {

CursorWidth RegularEditingEngine::cursor_width() const
{
    return CursorWidth::NARROW;
}

bool RegularEditingEngine::on_key(KeyEvent const& event)
{
    if (EditingEngine::on_key(event))
        return true;

    if (event.alt() && event.shift() && event.key() == KeyCode::Key_S) {
        sort_selected_lines();
        return true;
    }

    return false;
}

static int strcmp_utf32(u32 const* s1, u32 const* s2, size_t n)
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
        return strcmp_utf32(a->code_points(), b->code_points(), min(a->length(), b->length())) < 0;
    });

    m_editor->did_change();
    m_editor->update();
}

}
