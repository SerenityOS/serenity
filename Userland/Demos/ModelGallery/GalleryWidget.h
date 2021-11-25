/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BasicModel.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

class GalleryWidget final : public GUI::Widget {
    C_OBJECT(GalleryWidget)

private:
    GalleryWidget();

    ErrorOr<void> load_basic_model_tab();
    void load_sorting_filtering_tab();

    void add_textbox_contents_to_basic_model();

    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<GUI::Statusbar> m_statusbar;

    size_t m_invalidation_count { 0 };
    RefPtr<BasicModel> m_basic_model;
    RefPtr<GUI::TableView> m_basic_model_table;
    RefPtr<GUI::TextBox> m_new_item_name;
    RefPtr<GUI::Button> m_add_new_item;
    RefPtr<GUI::Button> m_remove_selected_item;
};
