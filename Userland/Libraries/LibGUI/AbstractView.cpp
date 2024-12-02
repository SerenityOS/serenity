/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/Timer.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/DragOperation.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Palette.h>

namespace GUI {

AbstractView::AbstractView()
    : m_sort_order(SortOrder::Ascending)
    , m_selection(*this)
{
    REGISTER_BOOL_PROPERTY("activates_on_selection", activates_on_selection, set_activates_on_selection);
    REGISTER_BOOL_PROPERTY("editable", is_editable, set_editable);
    REGISTER_BOOL_PROPERTY("searchable", is_searchable, set_searchable);
    REGISTER_ENUM_PROPERTY("selection_behavior", selection_behavior, set_selection_behavior, SelectionBehavior,
        { SelectionBehavior::SelectItems, "SelectItems" },
        { SelectionBehavior::SelectRows, "SelectRows" });
    REGISTER_ENUM_PROPERTY("selection_mode", selection_mode, set_selection_mode, SelectionMode,
        { SelectionMode::SingleSelection, "SingleSelection" },
        { SelectionMode::MultiSelection, "MultiSeleciton" },
        { SelectionMode::NoSelection, "NoSelection" });
    REGISTER_INT_PROPERTY("key_column", key_column, set_key_column);
    REGISTER_ENUM_PROPERTY("sort_order", sort_order, set_sort_order, SortOrder,
        { SortOrder::Ascending, "Ascending" },
        { SortOrder::Descending, "Descending" });
    REGISTER_BOOL_PROPERTY("tab_key_navigation_enabled", is_tab_key_navigation_enabled, set_tab_key_navigation_enabled);
    REGISTER_BOOL_PROPERTY("draw_item_text_with_shadow", does_draw_item_text_with_shadow, set_draw_item_text_with_shadow);

    set_focus_policy(GUI::FocusPolicy::StrongFocus);
}

AbstractView::~AbstractView()
{
    if (m_highlighted_search_timer)
        m_highlighted_search_timer->stop();
    if (m_model)
        m_model->unregister_view({}, *this);
}

void AbstractView::set_model(RefPtr<Model> model)
{
    if (model == m_model)
        return;
    if (m_model)
        m_model->unregister_view({}, *this);
    m_model = move(model);
    if (m_model)
        m_model->register_view({}, *this);
    model_did_update(GUI::Model::InvalidateAllIndices);
    scroll_to_top();
}

void AbstractView::model_did_update(unsigned int flags)
{
    if (!model() || (flags & GUI::Model::InvalidateAllIndices)) {
        stop_editing();
        m_edit_index = {};
        m_hovered_index = {};
        m_cursor_index = {};
        m_drop_candidate_index = {};
        clear_selection();
    } else {
        // FIXME: These may no longer point to whatever they did before,
        //        but let's be optimistic until we can be sure about it.
        if (!model()->is_within_range(m_edit_index)) {
            stop_editing();
            m_edit_index = {};
        }
        if (!model()->is_within_range(m_hovered_index))
            m_hovered_index = {};
        if (!model()->is_within_range(m_cursor_index))
            m_cursor_index = {};
        if (!model()->is_within_range(m_drop_candidate_index))
            m_drop_candidate_index = {};
        selection().remove_all_matching([this](auto& index) { return !model()->is_within_range(index); });

        if (m_highlighted_search.has_value()) {
            auto index = find_next_search_match(m_highlighted_search->view());
            if (index.is_valid())
                highlight_search(index);
        }
    }
    m_selection_start_index = {};
}

void AbstractView::clear_selection()
{
    m_selection.clear();
}

void AbstractView::set_selection(ModelIndex const& new_index)
{
    m_selection.set(new_index);
}

void AbstractView::set_selection_start_index(ModelIndex const& new_index)
{
    m_selection_start_index = new_index;
}

void AbstractView::add_selection(ModelIndex const& new_index)
{
    m_selection.add(new_index);
}

void AbstractView::remove_selection(ModelIndex const& new_index)
{
    m_selection.remove(new_index);
}

void AbstractView::toggle_selection(ModelIndex const& new_index)
{
    m_selection.toggle(new_index);
}

void AbstractView::did_update_selection()
{
    if (!model() || selection().first() != m_edit_index)
        stop_editing();
    if (model() && on_selection_change)
        on_selection_change();
}

void AbstractView::did_scroll()
{
    update_edit_widget_position();
}

void AbstractView::update_edit_widget_position()
{
    if (!m_edit_widget)
        return;
    m_edit_widget->set_relative_rect(m_edit_widget_content_rect.translated(-horizontal_scrollbar().value(), -vertical_scrollbar().value()));
}

void AbstractView::begin_editing(ModelIndex const& index)
{
    VERIFY(is_editable());
    VERIFY(model());
    if (m_edit_index == index)
        return;
    if (!model()->is_editable(index))
        return;
    if (m_edit_widget) {
        remove_child(*m_edit_widget);
        m_edit_widget = nullptr;
    }
    m_edit_index = index;

    VERIFY(aid_create_editing_delegate);
    m_editing_delegate = aid_create_editing_delegate(index);
    m_editing_delegate->bind(*model(), index);
    m_editing_delegate->set_value(index.data());
    m_edit_widget = m_editing_delegate->widget();
    add_child(*m_edit_widget);
    m_edit_widget->move_to_back();
    m_edit_widget_content_rect = editing_rect(index).translated(frame_thickness(), frame_thickness());
    update_edit_widget_position();
    m_edit_widget->set_focus(true);
    m_editing_delegate->will_begin_editing();
    m_editing_delegate->on_commit = [this] {
        VERIFY(model());
        model()->set_data(m_edit_index, m_editing_delegate->value());
        stop_editing();
    };
    m_editing_delegate->on_rollback = [this] {
        VERIFY(model());
        stop_editing();
    };
    m_editing_delegate->on_change = [this, index] {
        editing_widget_did_change(index);
    };
}

void AbstractView::stop_editing()
{
    bool take_back_focus = false;
    m_edit_index = {};
    if (m_edit_widget) {
        take_back_focus = m_edit_widget->is_focused();
        remove_child(*m_edit_widget);
        m_edit_widget = nullptr;
    }
    if (take_back_focus)
        set_focus(true);
}

void AbstractView::activate(ModelIndex const& index)
{
    if (on_activation)
        on_activation(index);
}

void AbstractView::activate_selected()
{
    if (!on_activation)
        return;

    selection().for_each_index([this](auto& index) {
        on_activation(index);
    });
}

void AbstractView::notify_selection_changed(Badge<ModelSelection>)
{
    did_update_selection();
    if (!m_suppress_update_on_selection_change)
        update();
}

NonnullRefPtr<Gfx::Font const> AbstractView::font_for_index(ModelIndex const& index) const
{
    if (!model())
        return font();

    auto font_data = index.data(ModelRole::Font);
    if (font_data.is_font())
        return font_data.as_font();

    return font();
}

void AbstractView::mousedown_event(MouseEvent& event)
{
    AbstractScrollableWidget::mousedown_event(event);

    if (!model())
        return;

    if (event.button() == MouseButton::Primary)
        m_left_mousedown_position = event.position();

    auto index = index_at_event_position(event.position());
    m_might_drag = false;

    if (!index.is_valid()) {
        clear_selection();
    } else if (event.modifiers() & Mod_Ctrl) {
        set_cursor(index, SelectionUpdate::Ctrl);
    } else if (event.modifiers() & Mod_Shift) {
        set_cursor(index, SelectionUpdate::Shift);
    } else if (event.button() == MouseButton::Primary && m_selection.contains(index) && !m_model->drag_data_type().is_null()) {
        // We might be starting a drag, so don't throw away other selected items yet.
        m_might_drag = true;
    } else if (event.button() == MouseButton::Secondary) {
        set_cursor(index, SelectionUpdate::ClearIfNotSelected);
    } else {
        set_cursor(index, SelectionUpdate::Set);
        m_might_drag = true;
    }

    update();
}

void AbstractView::set_hovered_index(ModelIndex const& index)
{
    if (m_hovered_index == index)
        return;
    auto old_index = m_hovered_index;
    m_hovered_index = index;
    did_change_hovered_index(old_index, index);

    if (old_index.is_valid())
        update(to_widget_rect(paint_invalidation_rect(old_index)));

    if (index.is_valid())
        update(to_widget_rect(paint_invalidation_rect(index)));
}

void AbstractView::leave_event(Core::Event& event)
{
    AbstractScrollableWidget::leave_event(event);
    set_hovered_index({});
}

void AbstractView::mousemove_event(MouseEvent& event)
{
    if (!model())
        return AbstractScrollableWidget::mousemove_event(event);

    if (widget_inner_rect().contains(event.position())) {
        auto hovered_index = index_at_event_position(event.position());
        set_hovered_index(hovered_index);
    }

    auto data_type = m_model->drag_data_type();
    if (data_type.is_null())
        return AbstractScrollableWidget::mousemove_event(event);

    if (!m_might_drag)
        return AbstractScrollableWidget::mousemove_event(event);

    if (!(event.buttons() & MouseButton::Primary) || m_selection.is_empty()) {
        m_might_drag = false;
        return AbstractScrollableWidget::mousemove_event(event);
    }

    auto diff = event.position() - m_left_mousedown_position;
    auto distance_travelled_squared = diff.x() * diff.x() + diff.y() * diff.y();
    constexpr int drag_distance_threshold = 5;

    if (distance_travelled_squared <= drag_distance_threshold)
        return AbstractScrollableWidget::mousemove_event(event);

    VERIFY(!data_type.is_null());

    if (m_is_dragging)
        return;

    // An event might sneak in between us constructing the drag operation and the
    // event loop exec at the end of `drag_operation->exec()' if the user is fast enough.
    // Prevent this by just ignoring later drag initiations (until the current drag operation ends).
    TemporaryChange dragging { m_is_dragging, true };

    dbgln_if(DRAG_DEBUG, "Initiate drag!");
    auto drag_operation = DragOperation::construct();

    drag_operation->set_mime_data(m_model->mime_data(m_selection));

    auto outcome = drag_operation->exec();

    switch (outcome) {
    case DragOperation::Outcome::Accepted:
        dbgln_if(DRAG_DEBUG, "Drag was accepted!");
        break;
    case DragOperation::Outcome::Cancelled:
        dbgln_if(DRAG_DEBUG, "Drag was cancelled!");
        m_might_drag = false;
        break;
    default:
        VERIFY_NOT_REACHED();
        break;
    }
}

void AbstractView::mouseup_event(MouseEvent& event)
{
    AbstractScrollableWidget::mouseup_event(event);

    if (!model())
        return;

    set_automatic_scrolling_timer_active(false);

    if (m_might_drag) {
        // We were unsure about unselecting items other than the current one
        // in mousedown_event(), because we could be seeing a start of a drag.
        // Since we're here, it was not that; so fix up the selection now.
        auto index = index_at_event_position(event.position());
        if (index.is_valid()) {
            set_cursor(index, SelectionUpdate::Set, true);
        } else
            clear_selection();
        m_might_drag = false;
        update();
    }

    if (activates_on_selection())
        activate_selected();
}

void AbstractView::doubleclick_event(MouseEvent& event)
{
    if (!model())
        return;

    if (event.button() != MouseButton::Primary)
        return;

    m_might_drag = false;

    auto index = index_at_event_position(event.position());

    if (!index.is_valid()) {
        clear_selection();
        return;
    }

    if (!m_selection.contains(index))
        set_selection(index);

    if (is_editable() && edit_triggers() & EditTrigger::DoubleClicked)
        begin_editing(cursor_index());
    else
        activate(cursor_index());
}

void AbstractView::context_menu_event(ContextMenuEvent& event)
{
    if (!model())
        return;

    auto index = index_at_event_position(event.position());

    if (index.is_valid())
        add_selection(index);
    else
        clear_selection();

    if (on_context_menu_request)
        on_context_menu_request(index, event);
}

void AbstractView::drop_event(DropEvent& event)
{
    event.accept();

    if (!model())
        return;

    auto index = index_at_event_position(event.position());
    if (on_drop)
        on_drop(index, event);
}

void AbstractView::set_selection_mode(SelectionMode selection_mode)
{
    if (m_selection_mode == selection_mode)
        return;
    m_selection_mode = selection_mode;

    if (m_selection_mode == SelectionMode::NoSelection)
        m_selection.clear();
    else if (m_selection_mode != SelectionMode::SingleSelection && m_selection.size() > 1) {
        auto first_selected = m_selection.first();
        m_selection.clear();
        m_selection.set(first_selected);
    }

    update();
}

void AbstractView::set_key_column_and_sort_order(int column, SortOrder sort_order)
{
    m_key_column = column;
    m_sort_order = sort_order;

    if (model())
        model()->sort(column, sort_order);

    update();
}

void AbstractView::select_range(ModelIndex const& index)
{
    auto min_row = min(selection_start_index().row(), index.row());
    auto max_row = max(selection_start_index().row(), index.row());
    auto min_column = min(selection_start_index().column(), index.column());
    auto max_column = max(selection_start_index().column(), index.column());

    clear_selection();
    for (auto row = min_row; row <= max_row; ++row) {
        for (auto column = min_column; column <= max_column; ++column) {
            auto new_index = model()->index(row, column);
            if (new_index.is_valid())
                toggle_selection(new_index);
        }
    }
}

void AbstractView::set_cursor(ModelIndex index, SelectionUpdate selection_update, bool scroll_cursor_into_view)
{
    if (!model() || !index.is_valid() || selection_mode() == SelectionMode::NoSelection) {
        m_cursor_index = {};
        stop_highlighted_search_timer();
        return;
    }

    if (!m_cursor_index.is_valid() || model()->parent_index(m_cursor_index) != model()->parent_index(index))
        stop_highlighted_search_timer();

    if (selection_mode() == SelectionMode::SingleSelection && (selection_update == SelectionUpdate::Ctrl || selection_update == SelectionUpdate::Shift))
        selection_update = SelectionUpdate::Set;

    if (model()->is_within_range(index)) {
        if (selection_update == SelectionUpdate::Set) {
            set_selection(index);
            set_selection_start_index(index);
        } else if (selection_update == SelectionUpdate::Ctrl) {
            toggle_selection(index);
        } else if (selection_update == SelectionUpdate::ClearIfNotSelected) {
            if (!m_selection.contains(index))
                clear_selection();
        } else if (selection_update == SelectionUpdate::Shift) {
            if (!selection_start_index().is_valid())
                set_selection_start_index(index);
            select_range(index);
        }

        // FIXME: Support the other SelectionUpdate types

        auto old_cursor_index = m_cursor_index;
        m_cursor_index = index;
        did_change_cursor_index(old_cursor_index, m_cursor_index);

        if (scroll_cursor_into_view)
            scroll_into_view(index, true, true);
        update();
    }
}

void AbstractView::set_edit_triggers(unsigned triggers)
{
    m_edit_triggers = triggers;
}

void AbstractView::hide_event(HideEvent& event)
{
    stop_editing();
    AbstractScrollableWidget::hide_event(event);
}

void AbstractView::keydown_event(KeyEvent& event)
{
    if (event.alt()) {
        event.ignore();
        return;
    }

    if (event.key() == KeyCode::Key_F2) {
        if (is_editable() && edit_triggers() & EditTrigger::EditKeyPressed) {
            begin_editing(cursor_index());
            event.accept();
            return;
        }
    }

    if (event.key() == KeyCode::Key_Return) {
        activate_selected();
        event.accept();
        return;
    }

    SelectionUpdate selection_update = SelectionUpdate::Set;
    if (event.modifiers() == KeyModifier::Mod_Shift) {
        selection_update = SelectionUpdate::Shift;
    }

    if (event.key() == KeyCode::Key_Left) {
        move_cursor(CursorMovement::Left, selection_update);
        event.accept();
        return;
    }
    if (event.key() == KeyCode::Key_Right) {
        move_cursor(CursorMovement::Right, selection_update);
        event.accept();
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        move_cursor(CursorMovement::Up, selection_update);
        event.accept();
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        move_cursor(CursorMovement::Down, selection_update);
        event.accept();
        return;
    }
    if (event.key() == KeyCode::Key_Home) {
        move_cursor(CursorMovement::Home, selection_update);
        event.accept();
        return;
    }
    if (event.key() == KeyCode::Key_End) {
        move_cursor(CursorMovement::End, selection_update);
        event.accept();
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        move_cursor(CursorMovement::PageUp, selection_update);
        event.accept();
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        move_cursor(CursorMovement::PageDown, selection_update);
        event.accept();
        return;
    }

    if (is_searchable()) {
        if (event.key() == KeyCode::Key_Backspace) {
            if (m_highlighted_search.has_value()) {
                // if (event.modifiers() == Mod_Ctrl) {
                //  TODO: delete last word
                // }
                Utf8View view(*m_highlighted_search);
                size_t n_code_points = view.length();
                if (n_code_points > 1) {
                    n_code_points--;
                    StringBuilder sb;
                    for (auto it = view.begin(); it != view.end(); ++it) {
                        if (n_code_points == 0)
                            break;
                        n_code_points--;
                        sb.append_code_point(*it);
                    }
                    auto index = find_next_search_match(sb.string_view());
                    if (index.is_valid()) {
                        m_highlighted_search = sb.to_byte_string();
                        highlight_search(index);
                        start_highlighted_search_timer();
                    }
                } else {
                    stop_highlighted_search_timer();
                }

                event.accept();
                return;
            }
        } else if (event.key() == KeyCode::Key_Escape) {
            if (m_highlighted_search.has_value()) {
                stop_highlighted_search_timer();

                event.accept();
                return;
            }
        } else if (event.key() != KeyCode::Key_Tab && !event.ctrl() && !event.alt() && event.code_point() != 0) {
            StringBuilder sb;
            if (m_highlighted_search.has_value())
                sb.append(*m_highlighted_search);
            sb.append_code_point(event.code_point());

            auto index = find_next_search_match(sb.string_view());
            if (index.is_valid()) {
                m_highlighted_search = sb.to_byte_string();
                highlight_search(index);
                start_highlighted_search_timer();
                set_cursor(index, SelectionUpdate::None, true);
            }

            event.accept();
            return;
        }
    }

    AbstractScrollableWidget::keydown_event(event);
}

void AbstractView::stop_highlighted_search_timer()
{
    m_highlighted_search.clear();
    if (m_highlighted_search_timer)
        m_highlighted_search_timer->stop();
    if (m_highlighted_search_index.is_valid()) {
        m_highlighted_search_index = {};
        update();
    }
}

void AbstractView::start_highlighted_search_timer()
{
    if (!m_highlighted_search_timer) {
        m_highlighted_search_timer = add<Core::Timer>();
        m_highlighted_search_timer->set_single_shot(true);
        m_highlighted_search_timer->on_timeout = [this] {
            stop_highlighted_search_timer();
        };
    }
    m_highlighted_search_timer->set_interval(5 * 1000);
    m_highlighted_search_timer->restart();
}

ModelIndex AbstractView::find_next_search_match(StringView const search)
{
    if (search.is_empty())
        return {};

    auto found_indices = model()->matches(search, Model::MatchesFlag::FirstMatchOnly | Model::MatchesFlag::MatchAtStart | Model::MatchesFlag::CaseInsensitive, model()->parent_index(cursor_index()));

    if (found_indices.is_empty())
        return {};

    return found_indices[0];
}

void AbstractView::highlight_search(ModelIndex const index)
{
    m_highlighted_search_index = index;
    set_selection(index);
    scroll_into_view(index);
    update();
}

bool AbstractView::is_searchable() const
{
    if (!m_searchable || !model())
        return false;
    return model()->is_searchable();
}

void AbstractView::set_searchable(bool searchable)
{
    if (m_searchable == searchable)
        return;
    m_searchable = searchable;
    if (!m_searchable)
        stop_highlighted_search_timer();
}

void AbstractView::draw_item_text(Gfx::Painter& painter, ModelIndex const& index, bool is_selected, Gfx::IntRect const& text_rect, StringView item_text, Gfx::Font const& font, Gfx::TextAlignment alignment, Gfx::TextElision elision, size_t search_highlighting_offset)
{
    if (m_edit_index == index)
        return;

    Color text_color;
    if (!is_enabled())
        text_color = palette().color(Gfx::ColorRole::DisabledText);
    else if (is_selected)
        text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();
    else
        text_color = index.data(ModelRole::ForegroundColor).to_color(palette().color(foreground_role()));

    if (index == m_highlighted_search_index) {
        auto const byte_offset = search_highlighting_offset < m_highlighted_search.value_or("").length() ? 0 : item_text.length();
        auto const byte_length = min(item_text.length() - byte_offset, m_highlighted_search.value_or("").length() - search_highlighting_offset);
        Utf8View const searching_text(item_text.substring_view(byte_offset, byte_length));

        // Highlight the text background first
        auto highlight_rect = text_rect.shrunken(0, text_rect.height() - font.pixel_size_rounded_up() - 2);
        highlight_rect.set_width((int)font.width(searching_text));

        // If the text is center aligned the highlight rect needs to be shifted to the right so that the two line up
        if (alignment == Gfx::TextAlignment::Center)
            highlight_rect.translate_by((text_rect.width() - (int)font.width(item_text)) / 2, 0);

        painter.fill_rect(highlight_rect, palette().highlight_searching());

        // Then draw the text
        auto searching_text_it = searching_text.begin();
        while (searching_text_it != searching_text.end() && is_ascii_space(*searching_text_it))
            ++searching_text_it;

        auto const highlight_text_color = palette().highlight_searching_text();
        painter.draw_text([&](auto const& rect, Utf8CodePointIterator& it) {
            if (searching_text_it != searching_text.end()) {
                do {
                    ++searching_text_it;
                } while (searching_text_it != searching_text.end() && is_ascii_space(*searching_text_it));

                painter.draw_glyph_or_emoji(rect.location(), it, font, highlight_text_color);
            } else {
                painter.draw_glyph_or_emoji(rect.location(), it, font, text_color);
            }
        },
            text_rect, item_text, font, alignment, elision);
    } else {
        if (m_draw_item_text_with_shadow) {
            painter.draw_text(text_rect.translated(1, 1), item_text, font, alignment, Color::Black, elision);
            painter.draw_text(text_rect, item_text, font, alignment, Color::White, elision);
        } else {
            painter.draw_text(text_rect, item_text, font, alignment, text_color, elision);
        }
    }
}

void AbstractView::focusin_event(FocusEvent& event)
{
    AbstractScrollableWidget::focusin_event(event);

    if (model() && !cursor_index().is_valid()) {
        move_cursor(CursorMovement::Home, SelectionUpdate::None);
        clear_selection();
    }
}

void AbstractView::drag_enter_event(DragEvent& event)
{
    if (!model())
        return;

    if (!is_editable())
        return;

    // NOTE: Right now, AbstractView accepts drags since we won't get "drag move" events
    //       unless we accept the "drag enter" event.
    //       We might be able to reduce event traffic by communicating the set of drag-accepting
    //       rects in this widget to the windowing system somehow.
    event.accept();
    dbgln_if(DRAG_DEBUG, "accepting drag of {}", event.mime_data().formats());
}

void AbstractView::drag_move_event(DragEvent& event)
{
    if (!model())
        return;

    auto index = index_at_event_position(event.position());
    ModelIndex new_drop_candidate_index;
    bool acceptable = model()->accepts_drag(index, event.mime_data());

    if (acceptable && index.is_valid())
        new_drop_candidate_index = index;

    if (acceptable) {
        m_automatic_scroll_delta = automatic_scroll_delta_from_position(event.position());
        set_automatic_scrolling_timer_active(!m_automatic_scroll_delta.is_zero());
    }

    if (m_drop_candidate_index != new_drop_candidate_index) {
        m_drop_candidate_index = new_drop_candidate_index;
        update();
    }
    if (m_drop_candidate_index.is_valid())
        event.accept();
}

void AbstractView::drag_leave_event(Event&)
{
    if (m_drop_candidate_index.is_valid()) {
        m_drop_candidate_index = {};
        update();
    }

    set_automatic_scrolling_timer_active(false);
}

void AbstractView::automatic_scrolling_timer_did_fire()
{
    if (m_automatic_scroll_delta.is_zero())
        return;

    vertical_scrollbar().increase_slider_by(m_automatic_scroll_delta.y());
    horizontal_scrollbar().increase_slider_by(m_automatic_scroll_delta.x());
}

}
