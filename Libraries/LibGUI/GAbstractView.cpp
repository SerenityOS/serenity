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
#include <Kernel/KeyCode.h>
#include <LibGUI/GAbstractView.h>
#include <LibGUI/GDragOperation.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GModelEditingDelegate.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GTextBox.h>

GAbstractView::GAbstractView(GWidget* parent)
    : GScrollableWidget(parent)
    , m_selection(*this)
{
}

GAbstractView::~GAbstractView()
{
}

void GAbstractView::set_model(RefPtr<GModel>&& model)
{
    if (model == m_model)
        return;
    if (m_model)
        m_model->unregister_view({}, *this);
    m_model = move(model);
    if (m_model)
        m_model->register_view({}, *this);
    did_update_model();
}

void GAbstractView::did_update_model()
{
    if (!model() || selection().first() != m_edit_index)
        stop_editing();
}

void GAbstractView::did_update_selection()
{
    if (!model() || selection().first() != m_edit_index)
        stop_editing();
    if (model() && on_selection && selection().first().is_valid())
        on_selection(selection().first());
}

void GAbstractView::did_scroll()
{
    update_edit_widget_position();
}

void GAbstractView::update_edit_widget_position()
{
    if (!m_edit_widget)
        return;
    m_edit_widget->set_relative_rect(m_edit_widget_content_rect.translated(-horizontal_scrollbar().value(), -vertical_scrollbar().value()));
}

void GAbstractView::begin_editing(const GModelIndex& index)
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
    m_editing_delegate->set_value(model()->data(index, GModel::Role::Display));
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
}

void GAbstractView::stop_editing()
{
    m_edit_index = {};
    if (m_edit_widget) {
        remove_child(*m_edit_widget);
        m_edit_widget = nullptr;
    }
}

void GAbstractView::select_all()
{
    ASSERT(model());
    int rows = model()->row_count();
    int columns = model()->column_count();

    for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < columns; ++j)
            selection().add(model()->index(i, j));
    }
}

void GAbstractView::activate(const GModelIndex& index)
{
    if (on_activation)
        on_activation(index);
}

void GAbstractView::activate_selected()
{
    if (!on_activation)
        return;

    selection().for_each_index([this](auto& index) {
        on_activation(index);
    });
}

void GAbstractView::notify_selection_changed(Badge<GModelSelection>)
{
    did_update_selection();
    if (on_selection_change)
        on_selection_change();
    update();
}

NonnullRefPtr<Font> GAbstractView::font_for_index(const GModelIndex& index) const
{
    if (!model())
        return font();

    auto font_data = model()->data(index, GModel::Role::Font);
    if (font_data.is_font())
        return font_data.as_font();

    auto column_metadata = model()->column_metadata(index.column());
    if (column_metadata.font)
        return *column_metadata.font;
    return font();
}

void GAbstractView::mousedown_event(GMouseEvent& event)
{
    GScrollableWidget::mousedown_event(event);

    if (!model())
        return;

    if (event.button() == GMouseButton::Left)
        m_left_mousedown_position = event.position();

    auto index = index_at_event_position(event.position());
    m_might_drag = false;

    if (!index.is_valid()) {
        m_selection.clear();
    } else if (event.modifiers() & Mod_Ctrl) {
        m_selection.toggle(index);
    } else if (event.button() == GMouseButton::Left) {
        // We might be starting a drag, so don't throw away other selected items yet.
        m_might_drag = true;
        m_selection.add(index);
    } else {
        m_selection.set(index);
    }

    update();
}

void GAbstractView::mousemove_event(GMouseEvent& event)
{
    if (!model() || !m_might_drag)
        return GScrollableWidget::mousemove_event(event);

    if (!(event.buttons() & GMouseButton::Left) || m_selection.is_empty()) {
        m_might_drag = false;
        return GScrollableWidget::mousemove_event(event);
    }

    auto diff = event.position() - m_left_mousedown_position;
    auto distance_travelled_squared = diff.x() * diff.x() + diff.y() * diff.y();
    constexpr int drag_distance_threshold = 5;

    if (distance_travelled_squared <= drag_distance_threshold)
        return GScrollableWidget::mousemove_event(event);

    dbg() << "Initiate drag!";
    auto drag_operation = GDragOperation::construct();

    RefPtr<GraphicsBitmap> bitmap;

    StringBuilder text_builder;
    StringBuilder data_builder;
    bool first = true;
    m_selection.for_each_index([&](auto& index) {
        auto text_data = m_model->data(index);
        if (!first)
            text_builder.append(", ");
        text_builder.append(text_data.to_string());

        auto drag_data = m_model->data(index, GModel::Role::DragData);
        data_builder.append(drag_data.to_string());
        data_builder.append('\n');

        first = false;

        if (!bitmap) {
            GVariant icon_data = model()->data(index, GModel::Role::Icon);
            if (icon_data.is_icon())
                bitmap = icon_data.as_icon().bitmap_for_size(32);
        }
    });

    drag_operation->set_text(text_builder.to_string());
    drag_operation->set_bitmap(bitmap);
    drag_operation->set_data("url-list", data_builder.to_string());

    auto outcome = drag_operation->exec();

    switch (outcome) {
    case GDragOperation::Outcome::Accepted:
        dbg() << "Drag was accepted!";
        break;
    case GDragOperation::Outcome::Cancelled:
        dbg() << "Drag was cancelled!";
        break;
    default:
        ASSERT_NOT_REACHED();
        break;
    }
}

void GAbstractView::mouseup_event(GMouseEvent& event)
{
    GScrollableWidget::mouseup_event(event);

    if (!model())
        return;

    if (m_might_drag) {
        // We were unsure about unselecting items other than the current one
        // in mousedown_event(), because we could be seeing a start of a drag.
        // Since we're here, it was not that; so fix up the selection now.
        auto index = index_at_event_position(event.position());
        if (index.is_valid())
            m_selection.set(index);
        else
            m_selection.clear();
        m_might_drag = false;
        update();
    }
}

void GAbstractView::doubleclick_event(GMouseEvent& event)
{
    if (!model())
        return;

    if (event.button() != GMouseButton::Left)
        return;

    m_might_drag = false;

    auto index = index_at_event_position(event.position());

    if (!index.is_valid())
        m_selection.clear();
    else if (!m_selection.contains(index))
        m_selection.set(index);

    activate_selected();
}

void GAbstractView::context_menu_event(GContextMenuEvent& event)
{
    if (!model())
        return;

    auto index = index_at_event_position(event.position());

    if (index.is_valid())
        m_selection.add(index);
    else
        selection().clear();

    if (on_context_menu_request)
        on_context_menu_request(index, event);
}
