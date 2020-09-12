/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/AbstractView.h>

namespace GUI {

class ListView : public AbstractView {
    C_OBJECT(ListView)
public:
    virtual ~ListView() override;

    int item_height() const { return 16; }

    bool alternating_row_colors() const { return m_alternating_row_colors; }
    void set_alternating_row_colors(bool b) { m_alternating_row_colors = b; }

    bool hover_highlighting() const { return m_hover_highlighting; }
    void set_hover_highlighting(bool b) { m_hover_highlighting = b; }

    int horizontal_padding() const { return m_horizontal_padding; }

    virtual void scroll_into_view(const ModelIndex& index, bool scroll_horizontally, bool scroll_vertically) override;

    Gfx::IntPoint adjusted_position(const Gfx::IntPoint&) const;

    virtual ModelIndex index_at_event_position(const Gfx::IntPoint&) const override;
    virtual Gfx::IntRect content_rect(const ModelIndex&) const override;

    int model_column() const { return m_model_column; }
    void set_model_column(int column) { m_model_column = column; }

    virtual void select_all() override;

    Function<void()> on_escape_pressed;

    virtual void move_cursor(CursorMovement, SelectionUpdate) override;
    void move_cursor_relative(int steps, SelectionUpdate);

protected:
    ListView();
    virtual void paint_list_item(Painter&, int row_index, int painted_item_index);

private:
    virtual void did_update_model(unsigned flags) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;

    Gfx::IntRect content_rect(int row) const;
    int item_count() const;
    void update_content_size();

    int m_horizontal_padding { 2 };
    int m_model_column { 0 };
    bool m_alternating_row_colors { true };
    bool m_hover_highlighting { false };
};

}
