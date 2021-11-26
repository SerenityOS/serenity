/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SidebarWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/TabWidget.h>

SidebarWidget::SidebarWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();
    set_enabled(false);

    auto& tab_bar = add<GUI::TabWidget>();

    auto& outline_container = tab_bar.add_tab<GUI::Widget>("Outline");
    outline_container.set_layout<GUI::VerticalBoxLayout>();
    outline_container.layout()->set_margins(4);

    m_outline_tree_view = outline_container.add<GUI::TreeView>();
    m_outline_tree_view->set_activates_on_selection(true);

    auto& thumbnails_container = tab_bar.add_tab<GUI::Widget>("Thumbnails");
    thumbnails_container.set_layout<GUI::VerticalBoxLayout>();
    thumbnails_container.layout()->set_margins(4);

    // FIXME: Add thumbnail previews
}
