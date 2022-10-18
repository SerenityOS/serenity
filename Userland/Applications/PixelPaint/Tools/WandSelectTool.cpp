/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WandSelectTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <AK/Queue.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>

namespace PixelPaint {

static void set_flood_selection(Gfx::Bitmap& bitmap, Image& image, Gfx::IntPoint const& start_position, Gfx::IntPoint const& selection_offset, int threshold, Selection::MergeMode merge_mode)
{
    VERIFY(bitmap.bpp() == 32);

    Mask selection_mask = Mask::empty(bitmap.rect());

    auto pixel_reached = [&](Gfx::IntPoint location) {
        selection_mask.set(selection_offset.x() + location.x(), selection_offset.y() + location.y(), 0xFF);
    };

    bitmap.flood_visit_from_point(start_position, threshold, move(pixel_reached));

    selection_mask.shrink_to_fit();
    image.selection().merge(selection_mask, merge_mode);
}

void WandSelectTool::on_keydown(GUI::KeyEvent& key_event)
{
    Tool::on_keydown(key_event);
    if (key_event.key() == KeyCode::Key_Escape) {
        m_editor->image().selection().clear();
    }
}

void WandSelectTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (!layer->rect().contains(layer_event.position()))
        return;

    auto selection_offset = layer->relative_rect().top_left();

    m_editor->image().selection().begin_interactive_selection();
    set_flood_selection(layer->currently_edited_bitmap(), m_editor->image(), layer_event.position(), selection_offset, m_threshold, m_merge_mode);
    m_editor->image().selection().end_interactive_selection();
    m_editor->update();
    m_editor->did_complete_action(tool_name());
}

GUI::Widget* WandSelectTool::get_properties_widget()
{
    if (m_properties_widget) {
        return m_properties_widget.ptr();
    }

    m_properties_widget = GUI::Widget::construct();
    m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

    auto& threshold_container = m_properties_widget->add<GUI::Widget>();
    threshold_container.set_fixed_height(20);
    threshold_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& threshold_label = threshold_container.add<GUI::Label>("Threshold:");
    threshold_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    threshold_label.set_fixed_size(80, 20);

    auto& threshold_slider = threshold_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%");
    threshold_slider.set_range(0, 100);
    threshold_slider.set_value(m_threshold);

    threshold_slider.on_change = [&](int value) {
        m_threshold = value;
    };
    set_primary_slider(&threshold_slider);

    auto& mode_container = m_properties_widget->add<GUI::Widget>();
    mode_container.set_fixed_height(20);
    mode_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& mode_label = mode_container.add<GUI::Label>();
    mode_label.set_text("Mode:");
    mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    mode_label.set_fixed_size(80, 20);

    for (int i = 0; i < (int)Selection::MergeMode::__Count; i++) {
        switch ((Selection::MergeMode)i) {
        case Selection::MergeMode::Set:
            m_merge_mode_names.append("Set");
            break;
        case Selection::MergeMode::Add:
            m_merge_mode_names.append("Add");
            break;
        case Selection::MergeMode::Subtract:
            m_merge_mode_names.append("Subtract");
            break;
        case Selection::MergeMode::Intersect:
            m_merge_mode_names.append("Intersect");
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    auto& mode_combo = mode_container.add<GUI::ComboBox>();
    mode_combo.set_only_allow_values_from_model(true);
    mode_combo.set_model(*GUI::ItemListModel<String>::create(m_merge_mode_names));
    mode_combo.set_selected_index((int)m_merge_mode);
    mode_combo.on_change = [this](auto&&, GUI::ModelIndex const& index) {
        VERIFY(index.row() >= 0);
        VERIFY(index.row() < (int)Selection::MergeMode::__Count);

        m_merge_mode = (Selection::MergeMode)index.row();
    };

    return m_properties_widget.ptr();
}

}
