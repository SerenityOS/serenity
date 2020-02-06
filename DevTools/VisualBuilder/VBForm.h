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

#include "VBWidget.h"
#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/Widget.h>

class VBForm : public GUI::Widget {
    C_OBJECT(VBForm)
    friend class VBWidget;
public:
    explicit VBForm(const String& name, GUI::Widget* parent = nullptr);
    virtual ~VBForm() override;

    static VBForm* current();

    String name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

    bool is_selected(const VBWidget&) const;
    VBWidget* widget_at(const Gfx::Point&);

    void set_should_snap_to_grip(bool snap) { m_should_snap_to_grid = snap; }
    bool should_snap_to_grid() const { return m_should_snap_to_grid; }

    void insert_widget(VBWidgetType);

    Function<void(VBWidget*)> on_widget_selected;

    void load_from_file(const String& path);
    void write_to_file(const String& path);
    void dump();

protected:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void second_paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

private:
    void grabber_mousedown_event(GUI::MouseEvent&, Direction grabber);
    void set_single_selected_widget(VBWidget*);
    void add_to_selection(VBWidget&);
    void remove_from_selection(VBWidget&);
    void delete_selected_widgets();
    template<typename Callback>
    void for_each_selected_widget(Callback);
    void set_cursor_type_from_grabber(Direction grabber);

    VBWidget* single_selected_widget();

    String m_name;
    int m_grid_size { 5 };
    bool m_should_snap_to_grid { true };
    NonnullRefPtrVector<VBWidget> m_widgets;
    HashMap<GUI::Widget*, VBWidget*> m_gwidget_map;
    HashTable<VBWidget*> m_selected_widgets;
    Gfx::Point m_transform_event_origin;
    Gfx::Point m_next_insertion_position;
    Direction m_resize_direction { Direction::None };
    Direction m_mouse_direction_type { Direction::None };
    RefPtr<GUI::Menu> m_context_menu;
};
