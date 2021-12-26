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

#include <AK/Vector.h>
#include <LibGUI/AbstractView.h>

namespace GUI {

class ColumnsView : public AbstractView {
    C_OBJECT(ColumnsView)
public:
    int model_column() const { return m_model_column; }
    void set_model_column(int column) { m_model_column = column; }

    virtual ModelIndex index_at_event_position(const Gfx::Point&) const override;

private:
    ColumnsView();
    virtual ~ColumnsView() override;
    void push_column(ModelIndex& parent_index);
    void update_column_sizes();

    int item_height() const { return 16; }
    int icon_size() const { return 16; }
    int icon_spacing() const { return 2; }
    int text_padding() const { return 2; }

    virtual void did_update_model() override;
    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent& event) override;
    virtual void keydown_event(KeyEvent& event) override;

    struct Column {
        ModelIndex parent_index;
        int width;
        // TODO: per-column vertical scroll?
    };

    Vector<Column> m_columns;
    int m_model_column { 0 };
};

}
