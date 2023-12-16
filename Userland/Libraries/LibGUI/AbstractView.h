/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelSelection.h>
#include <LibGfx/TextElision.h>

namespace GUI {

class AbstractView
    : public AbstractScrollableWidget
    , public ModelClient {

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

    enum class SelectionBehavior {
        SelectItems,
        SelectRows,
    };

    enum class SelectionMode {
        SingleSelection,
        MultiSelection,
        NoSelection,
    };

    virtual void move_cursor(CursorMovement, SelectionUpdate) { }

    void set_model(RefPtr<Model>);
    Model* model() { return m_model.ptr(); }
    Model const* model() const { return m_model.ptr(); }

    ModelSelection& selection() { return m_selection; }
    ModelSelection const& selection() const { return m_selection; }
    virtual void select_all() { }

    void activate(ModelIndex const&);
    void activate_selected();

    bool is_editable() const { return m_editable; }
    void set_editable(bool editable) { m_editable = editable; }

    bool is_searchable() const;
    void set_searchable(bool);

    enum EditTrigger {
        None = 0,
        DoubleClicked = 1 << 0,
        EditKeyPressed = 1 << 1,
        AnyKeyPressed = 1 << 2,
    };

    unsigned edit_triggers() const { return m_edit_triggers; }
    void set_edit_triggers(unsigned);

    SelectionBehavior selection_behavior() const { return m_selection_behavior; }
    void set_selection_behavior(SelectionBehavior behavior) { m_selection_behavior = behavior; }

    SelectionMode selection_mode() const { return m_selection_mode; }
    void set_selection_mode(SelectionMode);

    virtual void model_did_update(unsigned flags) override;
    virtual void did_update_selection();

    virtual Gfx::IntRect content_rect(ModelIndex const&) const { return {}; }
    virtual Gfx::IntRect editing_rect(ModelIndex const& index) const { return content_rect(index); }
    virtual Gfx::IntRect paint_invalidation_rect(ModelIndex const& index) const { return content_rect(index); }

    virtual ModelIndex index_at_event_position(Gfx::IntPoint) const { return {}; }
    void begin_editing(ModelIndex const&);
    void stop_editing();

    void set_activates_on_selection(bool b) { m_activates_on_selection = b; }
    bool activates_on_selection() const { return m_activates_on_selection; }

    Function<void()> on_selection_change;
    Function<void(ModelIndex const&)> on_activation;
    Function<void(ModelIndex const&, ContextMenuEvent const&)> on_context_menu_request;
    Function<void(ModelIndex const&, DropEvent const&)> on_drop;

    Function<OwnPtr<ModelEditingDelegate>(ModelIndex const&)> aid_create_editing_delegate;

    void notify_selection_changed(Badge<ModelSelection>);

    NonnullRefPtr<Gfx::Font const> font_for_index(ModelIndex const&) const;

    void set_key_column_and_sort_order(int column, SortOrder);

    int key_column() const { return m_key_column; }
    void set_key_column(int column) { set_key_column_and_sort_order(column, sort_order()); }
    SortOrder sort_order() const { return m_sort_order; }
    void set_sort_order(SortOrder order) { set_key_column_and_sort_order(key_column(), order); }

    virtual void scroll_into_view(ModelIndex const&, [[maybe_unused]] bool scroll_horizontally = true, [[maybe_unused]] bool scroll_vertically = true) { }

    ModelIndex const& cursor_index() const { return m_cursor_index; }
    ModelIndex const& selection_start_index() const { return m_selection_start_index; }
    void set_cursor(ModelIndex, SelectionUpdate, bool scroll_cursor_into_view = true);

    bool is_tab_key_navigation_enabled() const { return m_tab_key_navigation_enabled; }
    void set_tab_key_navigation_enabled(bool enabled) { m_tab_key_navigation_enabled = enabled; }

    void set_draw_item_text_with_shadow(bool b) { m_draw_item_text_with_shadow = b; }
    bool does_draw_item_text_with_shadow() const { return m_draw_item_text_with_shadow; }

protected:
    AbstractView();
    virtual ~AbstractView() override;

    virtual void keydown_event(KeyEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void drag_enter_event(DragEvent&) override;
    virtual void drag_move_event(DragEvent&) override;
    virtual void drag_leave_event(Event&) override;
    virtual void drop_event(DropEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void hide_event(HideEvent&) override;
    virtual void focusin_event(FocusEvent&) override;

    virtual void automatic_scrolling_timer_did_fire() override;

    virtual void clear_selection();
    virtual void set_selection(ModelIndex const&);
    virtual void set_selection_start_index(ModelIndex const&);
    virtual void add_selection(ModelIndex const&);
    virtual void remove_selection(ModelIndex const&);
    virtual void toggle_selection(ModelIndex const&);
    virtual void select_range(ModelIndex const&);
    virtual void did_change_hovered_index([[maybe_unused]] ModelIndex const& old_index, [[maybe_unused]] ModelIndex const& new_index) { }
    virtual void did_change_cursor_index([[maybe_unused]] ModelIndex const& old_index, [[maybe_unused]] ModelIndex const& new_index) { }
    virtual void editing_widget_did_change([[maybe_unused]] ModelIndex const& index) { }

    void draw_item_text(Gfx::Painter&, ModelIndex const&, bool, Gfx::IntRect const&, StringView, Gfx::Font const&, Gfx::TextAlignment, Gfx::TextElision, size_t search_highlighting_offset = 0);

    void set_suppress_update_on_selection_change(bool value) { m_suppress_update_on_selection_change = value; }

    virtual void did_scroll() override;
    void set_hovered_index(ModelIndex const&);
    void update_edit_widget_position();

    void stop_highlighted_search_timer();
    void start_highlighted_search_timer();
    ModelIndex find_next_search_match(StringView const);
    void highlight_search(ModelIndex const index);

    ModelIndex drop_candidate_index() const { return m_drop_candidate_index; }

    bool m_editable { false };
    bool m_searchable { true };
    RefPtr<Widget> m_edit_widget;
    Gfx::IntRect m_edit_widget_content_rect;
    OwnPtr<ModelEditingDelegate> m_editing_delegate;

    Gfx::IntPoint m_left_mousedown_position;
    bool m_might_drag { false };

    int m_key_column { -1 };
    SortOrder m_sort_order;

    ModelIndex m_edit_index;
    ModelIndex m_hovered_index;
    ModelIndex m_highlighted_search_index;

private:
    ModelIndex m_selection_start_index;
    ModelIndex m_cursor_index;
    ModelIndex m_drop_candidate_index;

    RefPtr<Model> m_model;
    ModelSelection m_selection;
    Optional<ByteString> m_highlighted_search;
    RefPtr<Core::Timer> m_highlighted_search_timer;
    SelectionBehavior m_selection_behavior { SelectionBehavior::SelectItems };
    SelectionMode m_selection_mode { SelectionMode::SingleSelection };
    unsigned m_edit_triggers { EditTrigger::DoubleClicked | EditTrigger::EditKeyPressed };
    bool m_activates_on_selection { false };
    bool m_tab_key_navigation_enabled { false };
    bool m_is_dragging { false };
    bool m_draw_item_text_with_shadow { false };
    bool m_suppress_update_on_selection_change { false };

    Gfx::IntPoint m_automatic_scroll_delta {};
};

}
