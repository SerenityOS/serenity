/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterGallery.h"
#include "FilterModel.h"
#include <Applications/PixelPaint/FilterGalleryGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

FilterGallery::FilterGallery(GUI::Window* parent_window, ImageEditor* editor)
    : GUI::Dialog(parent_window)
    , m_editor(editor)
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
    m_preview_checkbox = main_widget.find_descendant_of_type_named<GUI::CheckBox>("preview");

    VERIFY(m_filter_tree);
    VERIFY(apply_button);
    VERIFY(cancel_button);
    VERIFY(m_config_widget);
    VERIFY(m_preview_checkbox);

    auto filter_model = FilterModel::create(m_editor);
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

        if (m_preview_checkbox->is_checked()) {
            if (auto* layer = m_editor->active_layer()) {
                if (m_preview_bitmap)
                    restore_layer_bitmap(layer);
                m_preview_bitmap = clone_of_active_layer_bitmap(layer);
                apply_filter_inhibiting_undo_stack();
                m_editor->update();
            }
        }
    };

    apply_button->on_click = [this](auto) {
        if (!m_selected_filter) {
            done(ExecResult::ExecAborted);
            return;
        }

        if (!m_preview_bitmap)
            m_selected_filter->apply();
        done(ExecResult::ExecOK);
    };

    cancel_button->on_click = [this](auto) {
        if (auto* layer = m_editor->active_layer()) {
            if (m_preview_bitmap)
                restore_layer_bitmap(layer);
        }
        done(ExecResult::ExecCancel);
    };

    m_preview_checkbox->on_checked = [this](bool checked) {
        if (!m_editor)
            return;

        if (auto* layer = m_editor->active_layer()) {
            if (checked) {
                if (!m_selected_filter)
                    return;

                m_preview_bitmap = clone_of_active_layer_bitmap(layer);
                apply_filter_inhibiting_undo_stack();
                m_editor->update();
            } else {
                if (!m_preview_bitmap)
                    return;
                restore_layer_bitmap(layer);
                m_editor->update();
            }
        }
    };
}

void FilterGallery::restore_layer_bitmap(Layer* active_layer)
{
    if (m_preview_bitmap) {
        VERIFY(m_preview_bitmap->size() == active_layer->bitmap().size());

        int height = m_preview_bitmap->height();
        int width = m_preview_bitmap->width();

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                auto original_color = m_preview_bitmap->get_pixel(x, y);
                active_layer->bitmap().set_pixel(x, y, original_color);
            }
        }

        m_preview_bitmap = nullptr;
    }
}

void FilterGallery::apply_filter_inhibiting_undo_stack()
{
    m_editor->inhibit_undo_stack = true;
    m_selected_filter->apply();
    m_editor->inhibit_undo_stack = false;
}

RefPtr<Gfx::Bitmap> FilterGallery::clone_of_active_layer_bitmap(Layer* active_layer)
{
    auto bitmap_or_error = active_layer->bitmap().clone();
    if (bitmap_or_error.is_error())
        return nullptr;
    return bitmap_or_error.release_value();
}
}
