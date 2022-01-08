/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterGallery.h"
#include "FilterModel.h"
#include <Applications/PixelPaint/FilterGalleryGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
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
    auto preview_checkbox = main_widget.find_descendant_of_type_named<GUI::CheckBox>("preview");

    VERIFY(m_filter_tree);
    VERIFY(apply_button);
    VERIFY(cancel_button);
    VERIFY(m_config_widget);
    VERIFY(preview_checkbox);

    auto filter_model = FilterModel::create(editor);
    m_filter_tree->set_model(filter_model);
    m_filter_tree->expand_tree();

    m_filter_tree->on_selection_change = [this]() {
        auto selected_index = m_filter_tree->selection().first();
        if (!selected_index.is_valid())
            return;

        auto selected_filter = static_cast<const FilterModel::FilterInfo*>(selected_index.internal_data());
        if (selected_filter->type != FilterModel::FilterInfo::Type::Filter)
            return;

        m_selected_filter = selected_filter->filter;

        m_selected_filter_config_widget = m_selected_filter->get_settings_widget();
        m_config_widget->remove_all_children();
        m_config_widget->add_child(*m_selected_filter_config_widget);
    };

    apply_button->on_click = [this](auto) {
        if (!m_selected_filter) {
            done(ExecResult::ExecAborted);
            return;
        }

        m_selected_filter->apply();
        done(ExecResult::ExecOK);
    };

    cancel_button->on_click = [this](auto) {
        done(ExecResult::ExecCancel);
    };

    preview_checkbox->on_checked = [this, editor](bool checked) {
        if (!editor)
            return;

        if (auto* layer = editor->active_layer()) {
            if (checked) {
                auto preview_bitmap_or_error = layer->bitmap().clone();

                if (preview_bitmap_or_error.is_error())
                    return;

                m_preview_bitmap = preview_bitmap_or_error.release_value();

                if (!m_selected_filter)
                    return;

                m_selected_filter->apply();
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            } else {
                if (!m_preview_bitmap)
                    return;

                VERIFY(m_preview_bitmap->size() == layer->bitmap().size());

                int height = m_preview_bitmap->height();
                int width = m_preview_bitmap->width();

                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        auto original_color = m_preview_bitmap->get_pixel(x, y);
                        layer->bitmap().set_pixel(x, y, original_color);
                    }
                }
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    };
}

}
