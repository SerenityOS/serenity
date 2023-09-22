/*
 * Copyright (c) 2022-2023, the SerenityOS developers.
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
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
    auto mask_rect = Gfx::IntRect(polygon_delta, polygon_bitmap.size()).intersected(m_editor->image().rect());
    auto selection_mask = Mask::full(mask_rect);

    auto pixel_reached = [&](Gfx::IntPoint location) {
        auto point_to_set = location.translated(polygon_delta);
        if (mask_rect.contains(point_to_set))
            selection_mask.set(point_to_set, 0);
    };

    polygon_bitmap.flood_visit_from_point({ 0, 0 }, 0, move(pixel_reached));
    selection_mask.shrink_to_fit();
    m_editor->image().selection().merge(selection_mask, m_merge_mode);
}

void PolygonalSelectTool::process_polygon()
{
    // Determine minimum bounding box that can hold the polygon.
    auto top_left = m_polygon_points.at(0);
    auto bottom_right = m_polygon_points.at(0);

    for (auto point : m_polygon_points) {
        if (point.x() < top_left.x())
            top_left.set_x(point.x());
        if (point.x() > bottom_right.x())
            bottom_right.set_x(point.x());
        if (point.y() < top_left.y())
            top_left.set_y(point.y());
        if (point.y() > bottom_right.y())
            bottom_right.set_y(point.y());
    }

    top_left.translate_by(-1);
    auto polygon_rect = Gfx::IntRect::from_two_points(top_left, bottom_right);
    auto image_rect = m_editor->image().rect();
    if (!polygon_rect.intersects(image_rect)) {
        m_editor->image().selection().merge(Gfx::IntRect {}, m_merge_mode);
        return;
    }

    if (m_polygon_points.last() != m_polygon_points.first())
        m_polygon_points.append(m_polygon_points.first());

    // We want to paint the polygon into the bitmap such that there is an empty 1px border all the way around it
    // this ensures that we have a known pixel (0,0) that is outside the polygon.
    auto bitmap_rect = polygon_rect.inflated(2, 2);
    // FIXME: It should be possible to limit the size of the polygon bitmap to the size of the canvas, as that is
    //        the maximum possible size of the selection.
    auto polygon_bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, bitmap_rect.size());
    if (polygon_bitmap_or_error.is_error())
        return;

    auto polygon_bitmap = polygon_bitmap_or_error.release_value();
    Gfx::Painter polygon_painter(polygon_bitmap);
    for (size_t i = 0; i < m_polygon_points.size() - 1; i++) {
        auto line_start = m_polygon_points.at(i) - top_left;
        auto line_end = m_polygon_points.at(i + 1) - top_left;
        polygon_painter.draw_line(line_start, line_end, Color::Black);
    }

    flood_polygon_selection(polygon_bitmap, top_left);
}

void PolygonalSelectTool::on_mousedown(Layer*, MouseEvent& event)
{
    auto const& image_event = event.image_event();
    if (image_event.button() != GUI::MouseButton::Primary)
        return;
    if (!m_selecting) {
        m_polygon_points.clear();
        m_last_selecting_cursor_position = image_event.position();
    }

    m_selecting = true;

    auto new_point = image_event.position();
    if (!m_polygon_points.is_empty() && image_event.shift())
        new_point = Tool::constrain_line_angle(m_polygon_points.last(), new_point);

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
    if (!m_selecting)
        return;

    auto const& image_event = event.image_event();
    if (image_event.shift())
        m_last_selecting_cursor_position = Tool::constrain_line_angle(m_polygon_points.last(), image_event.position());
    else
        m_last_selecting_cursor_position = image_event.position();

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

void PolygonalSelectTool::on_second_paint(Layer const*, GUI::PaintEvent& event)
{
    if (!m_selecting)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());

    auto draw_preview_lines = [&](auto color, auto thickness) {
        for (size_t i = 0; i < m_polygon_points.size() - 1; i++) {
            auto preview_start = editor_stroke_position(m_polygon_points.at(i), 1);
            auto preview_end = editor_stroke_position(m_polygon_points.at(i + 1), 1);
            painter.draw_line(preview_start, preview_end, color, thickness);
        }

        auto last_line_start = editor_stroke_position(m_polygon_points.at(m_polygon_points.size() - 1), 1);
        auto last_line_stop = editor_stroke_position(m_last_selecting_cursor_position, 1);
        painter.draw_line(last_line_start, last_line_stop, color, thickness);
    };

    draw_preview_lines(Gfx::Color::Black, 3);
    draw_preview_lines(Gfx::Color::White, 1);
}

bool PolygonalSelectTool::on_keydown(GUI::KeyEvent& key_event)
{
    if (key_event.key() == KeyCode::Key_Escape) {
        if (m_selecting) {
            m_selecting = false;
            m_polygon_points.clear();
        } else {
            m_editor->image().selection().clear();
        }
        return true;
    }
    return Tool::on_keydown(key_event);
}

NonnullRefPtr<GUI::Widget> PolygonalSelectTool::get_properties_widget()
{
    if (m_properties_widget)
        return *m_properties_widget.ptr();

    auto properties_widget = GUI::Widget::construct();
    properties_widget->set_layout<GUI::VerticalBoxLayout>();

    auto& mode_container = properties_widget->add<GUI::Widget>();
    mode_container.set_fixed_height(20);
    mode_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& mode_label = mode_container.add<GUI::Label>();
    mode_label.set_text("Mode:"_string);
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

    m_properties_widget = properties_widget;
    return *m_properties_widget;
}

Gfx::IntPoint PolygonalSelectTool::point_position_to_preferred_cell(Gfx::FloatPoint position) const
{
    return position.to_type<int>();
}

}
