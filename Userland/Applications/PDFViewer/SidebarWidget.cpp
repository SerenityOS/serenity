/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SidebarWidget.h"
#include "OutlineModel.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/TabWidget.h>

SidebarWidget::SidebarWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();
    set_enabled(false);

    auto& tab_bar = add<GUI::TabWidget>();

    auto& outline_container = tab_bar.add_tab<GUI::Widget>("Outline"_short_string);
    outline_container.set_layout<GUI::VerticalBoxLayout>(4);

    m_outline_tree_view = outline_container.add<GUI::TreeView>();
    m_outline_tree_view->set_activates_on_selection(true);
    m_outline_tree_view->set_should_fill_selected_rows(true);
    m_outline_tree_view->set_selection_behavior(GUI::AbstractView::SelectionBehavior::SelectRows);
    m_outline_tree_view->on_selection_change = [this]() {
        auto& selection = m_outline_tree_view->selection();
        if (selection.is_empty())
            return;
        auto destination = OutlineModel::get_destination(selection.first());
        on_destination_selected(destination);
    };

    auto& thumbnails_container = tab_bar.add_tab<GUI::Widget>("Thumbnails"_string);
    thumbnails_container.set_layout<GUI::VerticalBoxLayout>(4);

    // FIXME: Add thumbnail previews
}
