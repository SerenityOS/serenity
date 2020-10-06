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

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/DragOperation.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/TextBox.h>

namespace GUI {

AbstractView::AbstractView()
    : m_sort_order(SortOrder::Ascending)
    , m_selection(*this)
{
}

AbstractView::~AbstractView()
{
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
    did_update_model(GUI::Model::InvalidateAllIndexes);
    scroll_to_top();
}

void AbstractView::did_update_model(unsigned flags)
{
    // FIXME: It's unfortunate that we lose so much view state when the model updates in any way.
    stop_editing();
    m_edit_index = {};
    m_hovered_index = {};
    if (!model() || (flags & GUI::Model::InvalidateAllIndexes)) {
        clear_selection();
    } else {
        selection().remove_matching([this](auto& index) { return !model()->is_valid(index); });
    }
}

void AbstractView::clear_selection()
{
    m_selection.clear();
}

void AbstractView::set_selection(const ModelIndex& new_index)
{
    m_selection.set(new_index);
}

void AbstractView::add_selection(const ModelIndex& new_index)
{
    m_selection.add(new_index);
}

void AbstractView::remove_selection(const ModelIndex& new_index)
{
    m_selection.remove(new_index);
}

void AbstractView::toggle_selection(const ModelIndex& new_index)
{
    m_selection.toggle(new_index);
}

void AbstractView::did_update_selection()
{
    if (!model() || selection().first() != m_edit_index)
        stop_editing();
    if (model() && on_selection && selection().first().is_valid())
        on_selection(selection().first());
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

void AbstractView::begin_editing(const ModelIndex& index)
{
    ASSERT(is_editable());
    ASSERT(model());
    if (m_edit_index == index)
        return;
    if (!model()->is_editable(index))
        return;
    if (m_edit_widget) {
        remove_child(*m_edit_widget);
        m_edit_widget = nullptr;
    }
    m_edit_index = index;

    ASSERT(aid_create_editing_delegate);
    m_editing_delegate = aid_create_editing_delegate(index);
    m_editing_delegate->bind(*model(), index);
    m_editing_delegate->set_value(index.data());
    m_edit_widget = m_editing_delegate->widget();
    add_child(*m_edit_widget);
    m_edit_widget->move_to_back();
    m_edit_widget_content_rect = content_rect(index).translated(frame_thickness(), frame_thickness());
    update_edit_widget_position();
    m_edit_widget->set_focus(true);
    m_editing_delegate->will_begin_editing();
    m_editing_delegate->on_commit = [this] {
        ASSERT(model());
        model()->set_data(m_edit_index, m_editing_delegate->value());
        stop_editing();
    };
    m_editing_delegate->on_rollback = [this] {
        ASSERT(model());
        stop_editing();
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

void AbstractView::activate(const ModelIndex& index)
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
    if (on_selection_change)
        on_selection_change();
    update();
}

NonnullRefPtr<Gfx::Font> AbstractView::font_for_index(const ModelIndex& index) const
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
    ScrollableWidget::mousedown_event(event);

    if (!model())
        return;

    if (event.button() == MouseButton::Left)
        m_left_mousedown_position = event.position();

    auto index = index_at_event_position(event.position());
    m_might_drag = false;

    if (!index.is_valid()) {
        clear_selection();
    } else if (event.modifiers() & Mod_Ctrl) {
        set_cursor(index, SelectionUpdate::Ctrl);
    } else if (event.button() == MouseButton::Left && m_selection.contains(index) && !m_model->drag_data_type().is_null()) {
        // We might be starting a drag, so don't throw away other selected items yet.
        m_might_drag = true;
    } else if (event.button() == MouseButton::Right) {
        set_cursor(index, SelectionUpdate::ClearIfNotSelected);
    } else {
        set_cursor(index, SelectionUpdate::Set);
    }

    update();
}

void AbstractView::set_hovered_index(const ModelIndex& index)
{
    if (m_hovered_index == index)
        return;
    m_hovered_index = index;
    update();
}

void AbstractView::leave_event(Core::Event& event)
{
    ScrollableWidget::leave_event(event);
    set_hovered_index({});
}

void AbstractView::mousemove_event(MouseEvent& event)
{
    if (!model())
        return ScrollableWidget::mousemove_event(event);

    auto hovered_index = index_at_event_position(event.position());
    set_hovered_index(hovered_index);

    if (!m_might_drag)
        return ScrollableWidget::mousemove_event(event);

    if (!(event.buttons() & MouseButton::Left) || m_selection.is_empty()) {
        m_might_drag = false;
        return ScrollableWidget::mousemove_event(event);
    }

    auto diff = event.position() - m_left_mousedown_position;
    auto distance_travelled_squared = diff.x() * diff.x() + diff.y() * diff.y();
    constexpr int drag_distance_threshold = 5;

    if (distance_travelled_squared <= drag_distance_threshold)
        return ScrollableWidget::mousemove_event(event);

    auto data_type = m_model->drag_data_type();
    ASSERT(!data_type.is_null());

    dbg() << "Initiate drag!";
    auto drag_operation = DragOperation::construct();

    RefPtr<Gfx::Bitmap> bitmap;

    StringBuilder text_builder;
    StringBuilder data_builder;
    bool first = true;
    m_selection.for_each_index([&](auto& index) {
        auto text_data = index.data();
        if (!first)
            text_builder.append(", ");
        text_builder.append(text_data.to_string());

        auto drag_data = index.data(ModelRole::DragData);
        data_builder.append(drag_data.to_string());
        data_builder.append('\n');

        first = false;

        if (!bitmap) {
            Variant icon_data = index.data(ModelRole::Icon);
            if (icon_data.is_icon())
                bitmap = icon_data.as_icon().bitmap_for_size(32);
        }
    });

    drag_operation->set_text(text_builder.to_string());
    drag_operation->set_bitmap(bitmap);
    drag_operation->set_data(data_type, data_builder.to_string());

    auto outcome = drag_operation->exec();

    switch (outcome) {
    case DragOperation::Outcome::Accepted:
        dbg() << "Drag was accepted!";
        break;
    case DragOperation::Outcome::Cancelled:
        dbg() << "Drag was cancelled!";
        break;
    default:
        ASSERT_NOT_REACHED();
        break;
    }
}

void AbstractView::mouseup_event(MouseEvent& event)
{
    ScrollableWidget::mouseup_event(event);

    if (!model())
        return;

    if (m_might_drag) {
        // We were unsure about unselecting items other than the current one
        // in mousedown_event(), because we could be seeing a start of a drag.
        // Since we're here, it was not that; so fix up the selection now.
        auto index = index_at_event_position(event.position());
        if (index.is_valid())
            set_selection(index);
        else
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

    if (event.button() != MouseButton::Left)
        return;

    m_might_drag = false;

    auto index = index_at_event_position(event.position());

    if (!index.is_valid())
        clear_selection();
    else if (!m_selection.contains(index))
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

void AbstractView::set_multi_select(bool multi_select)
{
    if (m_multi_select == multi_select)
        return;
    m_multi_select = multi_select;
    if (!multi_select && m_selection.size() > 1) {
        auto first_selected = m_selection.first();
        m_selection.clear();
        m_selection.set(first_selected);
    }
}

void AbstractView::set_key_column_and_sort_order(int column, SortOrder sort_order)
{
    m_key_column = column;
    m_sort_order = sort_order;

    if (model())
        model()->sort(column, sort_order);

    update();
}

void AbstractView::set_cursor(ModelIndex index, SelectionUpdate selection_update, bool scroll_cursor_into_view)
{
    if (!model() || !index.is_valid()) {
        m_cursor_index = {};
        return;
    }

    if (model()->is_valid(index)) {
        if (selection_update == SelectionUpdate::Set)
            set_selection(index);
        else if (selection_update == SelectionUpdate::Ctrl)
            toggle_selection(index);
        else if (selection_update == SelectionUpdate::ClearIfNotSelected) {
            if (!m_selection.contains(index))
                clear_selection();
        }

        // FIXME: Support the other SelectionUpdate types

        m_cursor_index = index;

        if (scroll_cursor_into_view) {
            // FIXME: We should scroll into view both vertically *and* horizontally.
            scroll_into_view(index, false, true);
        }
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
    ScrollableWidget::hide_event(event);
}

void AbstractView::keydown_event(KeyEvent& event)
{
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

    Widget::keydown_event(event);
}

}
