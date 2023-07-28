/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Model.h>
#include <LibGUI/MultiView.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

REGISTER_WIDGET(GUI, MultiView)

namespace GUI {

MultiView::MultiView()
{
    set_active_widget(nullptr);
    set_grabbable_margins(2);
    m_icon_view = add<IconView>();
    m_table_view = add<TableView>();
    m_columns_view = add<ColumnsView>();

    for_each_view_implementation([&](auto& view) {
        view.set_should_hide_unnecessary_scrollbars(true);
        view.on_activation = [this](auto& index) {
            if (on_activation)
                on_activation(index);
        };
        view.on_selection_change = [this] {
            if (on_selection_change)
                on_selection_change();
        };
        view.on_context_menu_request = [this](auto& index, auto& event) {
            if (on_context_menu_request)
                on_context_menu_request(index, event);
        };
        view.on_drop = [this](auto& index, auto& event) {
            if (on_drop)
                on_drop(index, event);
        };
    });

    build_actions();
    set_view_mode(ViewMode::Icon);
}

MultiView::~MultiView() = default;

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
    VERIFY_NOT_REACHED();
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

void MultiView::set_column_visible(int column_index, bool visible)
{
    m_table_view->set_column_visible(column_index, visible);
}

void MultiView::build_actions()
{
    m_view_as_icons_action = Action::create_checkable(
        "Icon View", { Mod_Ctrl, KeyCode::Key_1 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/icon-view.png"sv).release_value_but_fixme_should_propagate_errors(), [this](auto&) {
            set_view_mode(ViewMode::Icon);
        },
        this);

    m_view_as_table_action = Action::create_checkable(
        "Table View", { Mod_Ctrl, KeyCode::Key_2 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/table-view.png"sv).release_value_but_fixme_should_propagate_errors(), [this](auto&) {
            set_view_mode(ViewMode::Table);
        },
        this);

    m_view_as_columns_action = Action::create_checkable(
        "Columns View", { Mod_Ctrl, KeyCode::Key_3 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/columns-view.png"sv).release_value_but_fixme_should_propagate_errors(), [this](auto&) {
            set_view_mode(ViewMode::Columns);
        },
        this);

    m_view_type_action_group = make<ActionGroup>();
    m_view_type_action_group->set_exclusive(true);
    m_view_type_action_group->add_action(*m_view_as_icons_action);
    m_view_type_action_group->add_action(*m_view_as_table_action);
    m_view_type_action_group->add_action(*m_view_as_columns_action);
}

AbstractView::SelectionMode MultiView::selection_mode() const
{
    return m_table_view->selection_mode();
}

void MultiView::set_selection_mode(AbstractView::SelectionMode selection_mode)
{
    m_table_view->set_selection_mode(selection_mode);
    m_icon_view->set_selection_mode(selection_mode);
    m_columns_view->set_selection_mode(selection_mode);
}

void MultiView::set_key_column_and_sort_order(int column, SortOrder sort_order)
{
    for_each_view_implementation([&](auto& view) {
        view.set_key_column_and_sort_order(column, sort_order);
    });
}

}
