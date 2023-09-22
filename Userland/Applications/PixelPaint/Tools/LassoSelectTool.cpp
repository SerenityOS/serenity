/*
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LassoSelectTool.h"
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

void LassoSelectTool::on_mousedown(Layer*, MouseEvent& event)
{
    if (!m_editor)
        return;

    auto const& image_event = event.image_event();
    m_start_position = image_event.position();
    m_most_recent_position = image_event.position();
    m_top_left = m_start_position;
    m_bottom_right = m_start_position;
    m_path_points.clear();
    m_path_points.append(m_most_recent_position);

    m_selecting = true;

    m_editor->image().selection().begin_interactive_selection();
}

void LassoSelectTool::on_mousemove(Layer*, MouseEvent& event)
{
    if (!m_selecting)
        return;

    auto const& image_event = event.image_event();
    auto new_position = image_event.position();

    if (new_position == m_most_recent_position)
        return;

    // tracking the bounding box for cropping the selection bitmap at the end
    if (new_position.x() < m_top_left.x())
        m_top_left.set_x(new_position.x());
    if (new_position.y() < m_top_left.y())
        m_top_left.set_y(new_position.y());
    if (new_position.x() > m_bottom_right.x())
        m_bottom_right.set_x(new_position.x());
    if (new_position.y() > m_bottom_right.y())
        m_bottom_right.set_y(new_position.y());

    m_path_points.append(new_position);

    m_most_recent_position = new_position;
}

void LassoSelectTool::on_mouseup(Layer*, MouseEvent&)
{
    if (!m_selecting)
        return;

    m_selecting = false;
    m_top_left.translate_by(-1);

    auto image_rect = m_editor->image().rect();
    auto lasso_rect = Gfx::IntRect::from_two_points(m_top_left, m_bottom_right);
    if (!lasso_rect.intersects(image_rect)) {
        m_editor->image().selection().merge(Gfx::IntRect {}, m_merge_mode);
        return;
    }

    if (m_path_points.last() != m_start_position)
        m_path_points.append(m_start_position);

    // We create a bitmap that is bigger by 1 pixel on each side
    auto lasso_bitmap_rect = lasso_rect.inflated(2, 2);
    // FIXME: It should be possible to limit the size of the lasso bitmap to the size of the canvas, as that is
    //        the maximum possible size of the selection.
    auto lasso_bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, lasso_bitmap_rect.size());
    if (lasso_bitmap_or_error.is_error())
        return;

    auto lasso_bitmap = lasso_bitmap_or_error.release_value();
    auto lasso_painter = Gfx::Painter(lasso_bitmap);
    for (size_t i = 0; i < m_path_points.size() - 1; i++) {
        auto start = m_path_points.at(i) - m_top_left;
        auto end = m_path_points.at(i + 1) - m_top_left;
        lasso_painter.draw_line(start, end, Color::Black, 1);
    }

    flood_lasso_selection(lasso_bitmap);
}

void LassoSelectTool::flood_lasso_selection(Gfx::Bitmap& lasso_bitmap)
{
    VERIFY(lasso_bitmap.bpp() == 32);

    // Create Mask which will track already-processed pixels
    auto mask_rect = Gfx::IntRect(m_top_left, lasso_bitmap.size()).intersected(m_editor->image().rect());
    auto selection_mask = Mask::full(mask_rect);

    auto pixel_reached = [&](Gfx::IntPoint location) {
        auto point_to_set = location.translated(m_top_left);
        if (mask_rect.contains(point_to_set))
            selection_mask.set(point_to_set, 0);
    };

    lasso_bitmap.flood_visit_from_point({ 0, 0 }, 0, move(pixel_reached));

    selection_mask.shrink_to_fit();
    m_editor->image().selection().merge(selection_mask, m_merge_mode);
}

void LassoSelectTool::on_second_paint(Layer const*, GUI::PaintEvent& event)
{
    if (!m_selecting || m_path_points.size() < 2)
        return;
    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());

    auto draw_preview_lines = [&](auto color, auto thickness) {
        for (size_t i = 0; i < m_path_points.size() - 1; i++) {
            auto preview_start = editor_stroke_position(m_path_points.at(i), 1);
            auto preview_end = editor_stroke_position(m_path_points.at(i + 1), 1);
            painter.draw_line(preview_start, preview_end, color, thickness);
        }
    };

    draw_preview_lines(Gfx::Color::Black, 3);
    draw_preview_lines(Gfx::Color::White, 1);
}

bool LassoSelectTool::on_keydown(GUI::KeyEvent& key_event)
{
    Tool::on_keydown(key_event);
    if (key_event.key() == KeyCode::Key_Escape) {
        if (m_selecting) {
            m_selecting = false;
            m_path_points.clear();
            return true;
        }
    }

    return Tool::on_keydown(key_event);
}

NonnullRefPtr<GUI::Widget> LassoSelectTool::get_properties_widget()
{
    if (m_properties_widget) {
        return *m_properties_widget.ptr();
    }

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

}
