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

#include <Kernel/KeyCode.h>
#include <LibGUI/GAbstractView.h>
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
