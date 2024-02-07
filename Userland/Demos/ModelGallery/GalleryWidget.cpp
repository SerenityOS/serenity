/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GalleryWidget.h"
#include <Demos/ModelGallery/BasicModelTabGML.h>

GalleryWidget::GalleryWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    auto& inner_widget = add<GUI::Widget>();
    inner_widget.set_layout<GUI::VerticalBoxLayout>(4);

    m_tab_widget = inner_widget.add<GUI::TabWidget>();
    m_statusbar = add<GUI::Statusbar>();

    (void)load_basic_model_tab();
    load_sorting_filtering_tab();
}

ErrorOr<void> GalleryWidget::load_basic_model_tab()
{
    auto& tab = m_tab_widget->add_tab<GUI::Widget>("Basic Model"_string);
    TRY(tab.load_from_gml(basic_model_tab_gml));

    m_basic_model = BasicModel::create();
    m_basic_model_table = *tab.find_descendant_of_type_named<GUI::TableView>("model_table");
    m_basic_model_table->set_model(m_basic_model);

    m_basic_model->on_invalidate = [&] {
        m_invalidation_count++;
        m_statusbar->set_text(String::formatted("Times invalidated: {}", m_invalidation_count).release_value_but_fixme_should_propagate_errors());
    };

    m_statusbar->set_text(TRY(String::formatted("Times invalidated: {}", m_invalidation_count)));

    m_basic_model->add_item("Well...");
    m_basic_model->add_item("...hello...");
    m_basic_model->add_item("...friends! :^)");

    m_new_item_name = *tab.find_descendant_of_type_named<GUI::TextBox>("new_item_name");
    m_add_new_item = *tab.find_descendant_of_type_named<GUI::Button>("add_new_item");
    m_remove_selected_item = *tab.find_descendant_of_type_named<GUI::Button>("remove_selected_item");

    m_add_new_item->set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/plus.png"sv)));
    m_remove_selected_item->set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/minus.png"sv)));

    m_new_item_name->on_return_pressed = [&] { add_textbox_contents_to_basic_model(); };
    m_add_new_item->on_click = [&](auto) { add_textbox_contents_to_basic_model(); };

    m_remove_selected_item->on_click = [&](auto) {
        auto index = m_basic_model_table->cursor_index();
        if (index.is_valid()) {
            m_basic_model->remove_item(index);
        }
    };

    return {};
}

void GalleryWidget::load_sorting_filtering_tab()
{
    // TODO: Add the SortingFilteringProxyModel here.
}

void GalleryWidget::add_textbox_contents_to_basic_model()
{
    if (!m_new_item_name->current_line().is_empty()) {
        m_basic_model->add_item(m_new_item_name->current_line().to_utf8());
        m_new_item_name->set_text(""sv);
    }
}
