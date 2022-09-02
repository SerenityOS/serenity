/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PolygonalSelectTool.h"
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

void PolygonalSelectTool::flood_polygon_selection(Gfx::Bitmap& polygon_bitmap, Gfx::IntPoint polygon_delta)
{
    VERIFY(polygon_bitmap.bpp() == 32);

    // Create Mask which will track already-processed pixels.
    Mask selection_mask = Mask::full(polygon_bitmap.rect().translated(polygon_delta));

    auto pixel_reached = [&](Gfx::IntPoint location) {
        selection_mask.set(Gfx::IntPoint(location.x(), location.y()).translated(polygon_delta), 0);
    };

    polygon_bitmap.flood_visit_from_point({ polygon_bitmap.width() - 1, polygon_bitmap.height() - 1 }, 0, move(pixel_reached));

    selection_mask.shrink_to_fit();
    m_editor->image().selection().merge(selection_mask, m_merge_mode);
}

void PolygonalSelectTool::process_polygon()
{
    // Determine minimum bounding box that can hold the polygon.
    auto min_x_seen = m_polygon_points.at(0).x();
    auto max_x_seen = m_polygon_points.at(0).x();
    auto min_y_seen = m_polygon_points.at(0).y();
    auto max_y_seen = m_polygon_points.at(0).y();

    for (auto point : m_polygon_points) {
        if (point.x() < min_x_seen)
            min_x_seen = point.x();
        if (point.x() > max_x_seen)
            max_x_seen = point.x();
        if (point.y() < min_y_seen)
            min_y_seen = point.y();
        if (point.y() > max_y_seen)
            max_y_seen = point.y();
    }

    // We create a bitmap that is bigger by 1 pixel on each side (+2) and need to account for the 0 indexed
    // pixel positions (+1) so we make the bitmap size the delta of x/y min/max + 3.
    auto polygon_bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { (max_x_seen - min_x_seen) + 3, (max_y_seen - min_y_seen) + 3 });
    if (polygon_bitmap_or_error.is_error())
        return;

    auto polygon_bitmap = polygon_bitmap_or_error.release_value();

    auto polygon_painter = Gfx::Painter(polygon_bitmap);
    // We want to paint the polygon into the bitmap such that there is an empty 1px border all the way around it
    // this ensures that we have a known pixel (0,0) that is outside the polygon. Since the coordinates are relative
    // to the layer but the bitmap is cropped to the bounding rect of the polygon we need to offset our
    // points by the the negative of min x/y. And because we want a 1 px offset to the right and down, we + 1 this.
    auto polygon_bitmap_delta = Gfx::IntPoint(-min_x_seen + 1, -min_y_seen + 1);
    polygon_painter.translate(polygon_bitmap_delta);
    for (size_t i = 0; i < m_polygon_points.size() - 1; i++) {
        polygon_painter.draw_line(m_polygon_points.at(i), m_polygon_points.at(i + 1), Color::Black);
    }
    polygon_painter.draw_line(m_polygon_points.at(m_polygon_points.size() - 1), m_polygon_points.at(0), Color::Black);

    // Delta to use for mapping the bitmap back to layer coordinates. -1 to account for the right and down offset.
    auto bitmap_to_layer_delta = Gfx::IntPoint(min_x_seen + m_editor->active_layer()->location().x() - 1, min_y_seen + m_editor->active_layer()->location().y() - 1);
    flood_polygon_selection(polygon_bitmap, bitmap_to_layer_delta);
}
void PolygonalSelectTool::on_mousedown(Layer*, MouseEvent& event)
{
    auto& image_event = event.image_event();
    if (image_event.button() != GUI::MouseButton::Primary)
        return;
    if (!m_selecting) {
        m_polygon_points.clear();
        m_last_selecting_cursor_position = event.layer_event().position();
    }

    m_selecting = true;

    auto new_point = event.layer_event().position();

    // This point matches the first point exactly. Consider this polygon finished.
    if (m_polygon_points.size() > 0 && new_point == m_polygon_points.at(0)) {
        m_selecting = false;
        m_editor->image().selection().end_interactive_selection();
        process_polygon();
        m_editor->did_complete_action(tool_name());
        m_editor->update();
        return;
    }

    // Avoid adding the same point multiple times if the user clicks again without moving the mouse.
    if (m_polygon_points.size() > 0 && m_polygon_points.at(m_polygon_points.size() - 1) == new_point)
        return;

    m_polygon_points.append(new_point);
    m_editor->image().selection().begin_interactive_selection();

    m_editor->update();
}

void PolygonalSelectTool::on_mousemove(Layer*, MouseEvent& event)
{
    if (m_selecting)
        m_last_selecting_cursor_position = event.layer_event().position();
    m_editor->update();
}

void PolygonalSelectTool::on_doubleclick(Layer*, MouseEvent&)
{
    m_selecting = false;
    m_editor->image().selection().end_interactive_selection();
    process_polygon();
    m_editor->did_complete_action(tool_name());
    m_editor->update();
}

void PolygonalSelectTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (!m_selecting)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());

    painter.translate(editor_layer_location(*layer));

    for (size_t i = 0; i < m_polygon_points.size() - 1; i++) {
        auto preview_start = editor_stroke_position(m_polygon_points.at(i), 1);
        auto preview_end = editor_stroke_position(m_polygon_points.at(i + 1), 1);
        painter.draw_line(preview_start, preview_end, Color::Black, AK::max(m_editor->scale(), 1));
    }

    auto last_line_start = editor_stroke_position(m_polygon_points.at(m_polygon_points.size() - 1), 1);
    auto last_line_stop = editor_stroke_position(m_last_selecting_cursor_position, 1);
    painter.draw_line(last_line_start, last_line_stop, Color::Black, AK::max(m_editor->scale(), 1));
}

void PolygonalSelectTool::on_keydown(GUI::KeyEvent& key_event)
{
    Tool::on_keydown(key_event);
    if (key_event.key() == KeyCode::Key_Escape) {
        if (m_selecting) {
            m_selecting = false;
            m_polygon_points.clear();
        } else {
            m_editor->image().selection().clear();
        }
    }
}

GUI::Widget* PolygonalSelectTool::get_properties_widget()
{
    if (m_properties_widget)
        return m_properties_widget.ptr();

    m_properties_widget = GUI::Widget::construct();
    m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

    auto& mode_container = m_properties_widget->add<GUI::Widget>();
    mode_container.set_fixed_height(20);
    mode_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& mode_label = mode_container.add<GUI::Label>();
    mode_label.set_text("Mode:");
    mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    mode_label.set_fixed_size(80, 20);

    static constexpr auto s_merge_mode_names = [] {
        Array<StringView, (int)Selection::MergeMode::__Count> names;
        for (size_t i = 0; i < names.size(); i++) {
            switch ((Selection::MergeMode)i) {
            case Selection::MergeMode::Set:
                names[i] = "Set"sv;
                break;
            case Selection::MergeMode::Add:
                names[i] = "Add"sv;
                break;
            case Selection::MergeMode::Subtract:
                names[i] = "Subtract"sv;
                break;
            case Selection::MergeMode::Intersect:
                names[i] = "Intersect"sv;
                break;
            default:
                break;
            }
        }
        return names;
    }();

    auto& mode_combo = mode_container.add<GUI::ComboBox>();
    mode_combo.set_only_allow_values_from_model(true);
    mode_combo.set_model(*GUI::ItemListModel<StringView, decltype(s_merge_mode_names)>::create(s_merge_mode_names));
    mode_combo.set_selected_index((int)m_merge_mode);
    mode_combo.on_change = [this](auto&&, GUI::ModelIndex const& index) {
        VERIFY(index.row() >= 0);
        VERIFY(index.row() < (int)Selection::MergeMode::__Count);

        m_merge_mode = (Selection::MergeMode)index.row();
    };

    return m_properties_widget.ptr();
}

Gfx::IntPoint PolygonalSelectTool::point_position_to_preferred_cell(Gfx::FloatPoint const& position) const
{
    return position.to_type<int>();
}

}
