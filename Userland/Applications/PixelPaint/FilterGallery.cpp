/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterGallery.h"
#include "FilterTreeModel.h"
#include <Applications/PixelPaint/FilterGalleryGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

FilterGallery::FilterGallery(GUI::Window* parent_window, ImageEditor* editor)
    : GUI::Dialog(parent_window)
{
    set_title("Filter Gallery");
    set_icon(parent_window->icon());
    resize(400, 250);
    set_resizable(true);

    auto& main_widget = set_main_widget<GUI::Widget>();
    if (!main_widget.load_from_gml(filter_gallery_gml))
        VERIFY_NOT_REACHED();

    m_filter_tree = main_widget.find_descendant_of_type_named<GUI::TreeView>("tree_view");
    auto apply_button = main_widget.find_descendant_of_type_named<GUI::Button>("apply_button");
    auto cancel_button = main_widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
    m_config_widget = main_widget.find_descendant_of_type_named<GUI::Widget>("config_widget");
    m_preview_widget = main_widget.find_descendant_of_type_named<FilterPreviewWidget>("preview_widget");

    VERIFY(m_filter_tree);
    VERIFY(apply_button);
    VERIFY(cancel_button);
    VERIFY(m_config_widget);
    VERIFY(m_preview_widget);

    auto filter_tree_model = MUST(create_filter_tree_model(editor));
    m_filter_tree->set_model(filter_tree_model);
    m_filter_tree->expand_tree();

    m_filter_tree->on_selection_change = [this]() {
        auto selected_index = m_filter_tree->selection().first();
        if (!selected_index.is_valid()) {
            m_preview_widget->clear_filter();
            return;
        }

        auto& node = *static_cast<GUI::TreeViewModel::Node*>(selected_index.internal_data());
        if (!is<FilterNode>(node)) {
            m_preview_widget->clear_filter();
            return;
        }

        m_selected_filter = &static_cast<FilterNode&>(node).filter();
        m_selected_filter->on_settings_change = [&]() {
            m_preview_widget->set_filter(m_selected_filter);
        };
        m_preview_widget->set_filter(m_selected_filter);

        m_selected_filter_config_widget = m_selected_filter->get_settings_widget();
        m_config_widget->remove_all_children();
        m_config_widget->add_child(*m_selected_filter_config_widget);
    };

    m_preview_widget->set_bitmap(editor->active_layer()->content_bitmap().clone().release_value());

    apply_button->on_click = [this](auto) {
        if (!m_selected_filter) {
            done(ExecResult::Aborted);
            return;
        }

        m_selected_filter->apply();
        done(ExecResult::OK);
    };

    cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };
}

}
