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

static float color_distance_squared(Gfx::Color const& lhs, Gfx::Color const& rhs)
{
    int a = rhs.red() - lhs.red();
    int b = rhs.green() - lhs.green();
    int c = rhs.blue() - lhs.blue();
    int d = rhs.alpha() - lhs.alpha();
    return (a * a + b * b + c * c + d * d) / (4.0f * 255.0f * 255.0f);
}

static bool meets_wand_threshold(Gfx::IntPoint point, Gfx::Bitmap& bitmap, Gfx::Color const& target_color, float threshold_normalized_squared)
{
    auto pixel_color = bitmap.get_pixel<Gfx::StorageFormat::BGRA8888>(point.x(), point.y());
    bool meets_threshold = color_distance_squared(pixel_color, target_color) <= threshold_normalized_squared;
    return meets_threshold;
}

static void set_flood_selection(Gfx::Bitmap& bitmap, Image& image, Gfx::IntPoint const& start_position, Color target_color, int threshold, Selection::MergeMode merge_mode)
{
    VERIFY(bitmap.bpp() == 32);

    float threshold_normalized_squared = (threshold / 100.0f) * (threshold / 100.0f);

    // Create Mask which will track already-colored pixels
    Mask flood_mask = Mask::empty(bitmap.rect());
    Mask selection_mask = Mask::empty(bitmap.rect());
    Queue<Gfx::IntPoint> points_to_visit = Queue<Gfx::IntPoint>();

    points_to_visit.enqueue({ start_position.x(), start_position.y() });
    selection_mask.set(start_position.x(), start_position.y(), 0xFF);
    flood_mask.set(start_position.x(), start_position.y(), 0xFF);

    // This implements a non-recursive flood fill. This is a breadth-first search of paintable neighbors
    // As we find neighbors that are paintable we update their pixel, add them to the queue, and mark them in the mask
    while (!points_to_visit.is_empty()) {
        auto current_point = points_to_visit.dequeue();
        Gfx::IntPoint candidate_point = {};

        if (current_point.x() > 0) {
            candidate_point = current_point.moved_left(1);
            if (flood_mask.get(candidate_point.x(), candidate_point.y()) == 0 && meets_wand_threshold(candidate_point, bitmap, target_color, threshold_normalized_squared)) {
                points_to_visit.enqueue(candidate_point);
                selection_mask.set(candidate_point.x(), candidate_point.y(), 0xFF);
            }
            flood_mask.set(candidate_point.x(), candidate_point.y(), 0xFF);
        }

        if (current_point.x() < bitmap.width() - 1) {
            candidate_point = current_point.moved_right(1);
            if (flood_mask.get(candidate_point.x(), candidate_point.y()) == 0 && meets_wand_threshold(candidate_point, bitmap, target_color, threshold_normalized_squared)) {
                points_to_visit.enqueue(candidate_point);
                selection_mask.set(candidate_point.x(), candidate_point.y(), 0xFF);
            }
            flood_mask.set(candidate_point.x(), candidate_point.y(), 0xFF);
        }

        if (current_point.y() > 0) {
            candidate_point = current_point.moved_up(1);
            if (flood_mask.get(candidate_point.x(), candidate_point.y()) == 0 && meets_wand_threshold(candidate_point, bitmap, target_color, threshold_normalized_squared)) {
                points_to_visit.enqueue(candidate_point);
                selection_mask.set(candidate_point.x(), candidate_point.y(), 0xFF);
            }
            flood_mask.set(candidate_point.x(), candidate_point.y(), 0xFF);
        }

        if (current_point.y() < bitmap.height() - 1) {
            candidate_point = current_point.moved_down(1);
            if (flood_mask.get(candidate_point.x(), candidate_point.y()) == 0 && meets_wand_threshold(candidate_point, bitmap, target_color, threshold_normalized_squared)) {
                points_to_visit.enqueue(candidate_point);
                selection_mask.set(candidate_point.x(), candidate_point.y(), 0xFF);
            }
            flood_mask.set(candidate_point.x(), candidate_point.y(), 0xFF);
        }
    }
    selection_mask.shrink_to_fit();
    image.selection().merge(selection_mask, merge_mode);
}

void WandSelectTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (!layer->rect().contains(layer_event.position()))
        return;

    auto target_color = layer->currently_edited_bitmap().get_pixel(layer_event.x(), layer_event.y());
    m_editor->image().selection().begin_interactive_selection();
    set_flood_selection(layer->currently_edited_bitmap(), m_editor->image(), layer_event.position(), target_color, m_threshold, m_merge_mode);
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
