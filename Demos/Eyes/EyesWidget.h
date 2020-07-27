/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#pragma once

#include <LibGUI/Widget.h>
#include <LibGfx/Point.h>

class EyesWidget final : public GUI::Widget {
    C_OBJECT(EyesWidget)

public:
    virtual ~EyesWidget();
    void track_cursor_globally();

private:
    EyesWidget(int num_eyes, int full_rows, int extra)
        : m_full_rows(full_rows)
        , m_extra_columns(extra)
    {
        m_num_rows = m_extra_columns > 0 ? m_full_rows + 1 : m_full_rows;
        m_eyes_in_row = m_full_rows > 0 ? (num_eyes - m_extra_columns) / m_full_rows : m_extra_columns;
    }

    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;

    void render_eyeball(int row, int column, GUI::Painter&) const;
    Gfx::IntPoint pupil_center(Gfx::IntRect& eyeball_bounds) const;

    Gfx::IntPoint m_mouse_position;
    int m_eyes_in_row { -1 };
    int m_full_rows { -1 };
    int m_extra_columns { -1 };
    int m_num_rows { -1 };
};
