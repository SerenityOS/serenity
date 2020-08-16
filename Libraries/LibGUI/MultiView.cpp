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
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Model.h>
#include <LibGUI/MultiView.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

namespace GUI {

MultiView::MultiView()
{
    set_active_widget(nullptr);
    set_content_margins({ 2, 2, 2, 2 });
    m_icon_view = add<IconView>();
    m_table_view = add<TableView>();
    m_columns_view = add<ColumnsView>();

    m_icon_view->on_activation = [&](auto& index) {
        if (on_activation)
            on_activation(index);
    };
    m_columns_view->on_activation = [&](auto& index) {
        if (on_activation)
            on_activation(index);
    };
    m_table_view->on_activation = [&](auto& index) {
        if (on_activation)
            on_activation(index);
    };

    m_table_view->on_selection_change = [this] {
        if (on_selection_change)
            on_selection_change();
    };
    m_icon_view->on_selection_change = [this] {
        if (on_selection_change)
            on_selection_change();
    };
    m_columns_view->on_selection_change = [this] {
        if (on_selection_change)
            on_selection_change();
    };

    m_table_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(index, event);
    };
    m_icon_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(index, event);
    };
    m_columns_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(index, event);
    };

    m_table_view->on_drop = [this](auto& index, auto& event) {
        if (on_drop)
            on_drop(index, event);
    };
    m_icon_view->on_drop = [this](auto& index, auto& event) {
        if (on_drop)
            on_drop(index, event);
    };
    m_columns_view->on_drop = [this](auto& index, auto& event) {
        if (on_drop)
            on_drop(index, event);
    };

    build_actions();
    set_view_mode(ViewMode::Icon);
    apply_multi_select();
}

MultiView::~MultiView()
{
}

void MultiView::set_view_mode(ViewMode mode)
{
    if (m_view_mode == mode)
        return;
    m_view_mode = mode;
    update();
    if (mode == ViewMode::Table) {
        set_active_widget(m_table_view);
        m_view_as_table_action->set_checked(true);
        return;
    }
    if (mode == ViewMode::Columns) {
        set_active_widget(m_columns_view);
        m_view_as_columns_action->set_checked(true);
        return;
    }
    if (mode == ViewMode::Icon) {
        set_active_widget(m_icon_view);
        m_view_as_icons_action->set_checked(true);
        return;
    }
    ASSERT_NOT_REACHED();
}

void MultiView::set_model(RefPtr<Model> model)
{
    if (m_model == model)
        return;
    m_model = model;
    for_each_view_implementation([&](auto& view) {
        view.set_model(model);
    });
}

void MultiView::set_model_column(int column)
{
    if (m_model_column == column)
        return;
    m_model_column = column;
    m_icon_view->set_model_column(column);
    m_columns_view->set_model_column(column);
}

void MultiView::set_column_hidden(int column_index, bool hidden)
{
    m_table_view->set_column_hidden(column_index, hidden);
}

void MultiView::build_actions()
{
    m_view_as_table_action = Action::create_checkable(
        "Table view", Gfx::Bitmap::load_from_file("/res/icons/16x16/table-view.png"), [this](auto&) {
            set_view_mode(ViewMode::Table);
        });

    m_view_as_icons_action = Action::create_checkable(
        "Icon view", Gfx::Bitmap::load_from_file("/res/icons/16x16/icon-view.png"), [this](auto&) {
            set_view_mode(ViewMode::Icon);
        });

    m_view_as_columns_action = Action::create_checkable(
        "Columns view", Gfx::Bitmap::load_from_file("/res/icons/16x16/columns-view.png"), [this](auto&) {
            set_view_mode(ViewMode::Columns);
        });

    m_view_type_action_group = make<ActionGroup>();
    m_view_type_action_group->set_exclusive(true);
    m_view_type_action_group->add_action(*m_view_as_table_action);
    m_view_type_action_group->add_action(*m_view_as_icons_action);
    m_view_type_action_group->add_action(*m_view_as_columns_action);
}

void MultiView::apply_multi_select()
{
    m_table_view->set_multi_select(m_multi_select);
    m_icon_view->set_multi_select(m_multi_select);
    m_columns_view->set_multi_select(m_multi_select);
}

void MultiView::set_multi_select(bool multi_select)
{
    if (m_multi_select == multi_select)
        return;
    m_multi_select = multi_select;
    apply_multi_select();
}

void MultiView::set_key_column_and_sort_order(int column, SortOrder sort_order)
{
    for_each_view_implementation([&](auto& view) {
        view.set_key_column_and_sort_order(column, sort_order);
    });
}

}
