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

#include <AK/Function.h>
#include <LibGUI/ModelSelection.h>
#include <LibGUI/ScrollableWidget.h>

namespace GUI {

class AbstractView : public ScrollableWidget {
    C_OBJECT_ABSTRACT(AbstractView);

public:
    enum class CursorMovement {
        Up,
        Down,
        Left,
        Right,
        Home,
        End,
        PageUp,
        PageDown,
    };

    enum class SelectionUpdate {
        None,
        Set,
        Shift,
        Ctrl,
        ClearIfNotSelected
    };

    virtual void move_cursor(CursorMovement, SelectionUpdate) { }

    void set_model(RefPtr<Model>);
    Model* model() { return m_model.ptr(); }
    const Model* model() const { return m_model.ptr(); }

    ModelSelection& selection() { return m_selection; }
    const ModelSelection& selection() const { return m_selection; }
    virtual void select_all() { }

    bool is_editable() const { return m_editable; }
    void set_editable(bool editable) { m_editable = editable; }

    enum EditTrigger {
        None = 0,
        DoubleClicked = 1 << 0,
        EditKeyPressed = 1 << 1,
        AnyKeyPressed = 1 << 2,
    };

    unsigned edit_triggers() const { return m_edit_triggers; }
    void set_edit_triggers(unsigned);

    bool is_multi_select() const { return m_multi_select; }
    void set_multi_select(bool);

    virtual bool accepts_focus() const override { return true; }
    virtual void did_update_model(unsigned flags);
    virtual void did_update_selection();

    virtual Gfx::IntRect content_rect(const ModelIndex&) const { return {}; }
    virtual ModelIndex index_at_event_position(const Gfx::IntPoint&) const { return {}; }
    void begin_editing(const ModelIndex&);
    void stop_editing();

    void set_activates_on_selection(bool b) { m_activates_on_selection = b; }
    bool activates_on_selection() const { return m_activates_on_selection; }

    Function<void()> on_selection_change;
    Function<void(const ModelIndex&)> on_activation;
    Function<void(const ModelIndex&)> on_selection;
    Function<void(const ModelIndex&, const ContextMenuEvent&)> on_context_menu_request;
    Function<void(const ModelIndex&, const DropEvent&)> on_drop;

    Function<OwnPtr<ModelEditingDelegate>(const ModelIndex&)> aid_create_editing_delegate;

    void notify_selection_changed(Badge<ModelSelection>);

    NonnullRefPtr<Gfx::Font> font_for_index(const ModelIndex&) const;

    void set_key_column_and_sort_order(int column, SortOrder);

    int key_column() const { return m_key_column; }
    SortOrder sort_order() const { return m_sort_order; }

    virtual void scroll_into_view(const ModelIndex&, [[maybe_unused]] bool scroll_horizontally = true, [[maybe_unused]] bool scroll_vertically = true) { }

    const ModelIndex& cursor_index() const { return m_cursor_index; }
    void set_cursor(ModelIndex, SelectionUpdate, bool scroll_cursor_into_view = true);

    bool is_tab_key_navigation_enabled() const { return m_tab_key_navigation_enabled; }
    void set_tab_key_navigation_enabled(bool enabled) { m_tab_key_navigation_enabled = enabled; }

protected:
    AbstractView();
    virtual ~AbstractView() override;

    virtual void keydown_event(KeyEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void drop_event(DropEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void hide_event(HideEvent&) override;

    virtual void clear_selection();
    virtual void set_selection(const ModelIndex&);
    virtual void add_selection(const ModelIndex&);
    virtual void remove_selection(const ModelIndex&);
    virtual void toggle_selection(const ModelIndex&);

    virtual void did_scroll() override;
    void set_hovered_index(const ModelIndex&);
    void activate(const ModelIndex&);
    void activate_selected();
    void update_edit_widget_position();

    bool m_editable { false };
    ModelIndex m_edit_index;
    RefPtr<Widget> m_edit_widget;
    Gfx::IntRect m_edit_widget_content_rect;
    OwnPtr<ModelEditingDelegate> m_editing_delegate;

    Gfx::IntPoint m_left_mousedown_position;
    bool m_might_drag { false };

    ModelIndex m_hovered_index;

    int m_key_column { -1 };
    SortOrder m_sort_order;

private:
    RefPtr<Model> m_model;
    ModelSelection m_selection;
    ModelIndex m_cursor_index;
    unsigned m_edit_triggers { EditTrigger::DoubleClicked | EditTrigger::EditKeyPressed };
    bool m_activates_on_selection { false };
    bool m_multi_select { true };
    bool m_tab_key_navigation_enabled { false };
};

}
