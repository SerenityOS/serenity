/*
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
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

void LassoSelectTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (!layer->rect().contains(layer_event.position()))
        return;

    auto selection_bitmap_result = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, layer->content_bitmap().size());
    if (selection_bitmap_result.is_error())
        return;

    m_selection_bitmap = selection_bitmap_result.release_value();
    m_start_position = layer_event.position();
    m_most_recent_position = layer_event.position();
    m_top_left = m_start_position;
    m_bottom_right = m_start_position;
    m_preview_path.clear();
    m_preview_path.move_to(editor_stroke_position(m_most_recent_position, 1).to_type<float>());
    m_selection_bitmap->set_pixel(m_most_recent_position, Gfx::Color::Black);

    m_selecting = true;

    m_editor->image().selection().begin_interactive_selection();
}

void LassoSelectTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!m_selecting)
        return;

    auto& layer_event = event.layer_event();
    auto new_position = layer_event.position();
    if (!layer->rect().contains(new_position))
        return;

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

    auto preview_end = editor_stroke_position(new_position, 1);
    m_preview_path.line_to(preview_end.to_type<float>());

    auto selection_painter = Gfx::Painter(*m_selection_bitmap);
    selection_painter.draw_line(m_most_recent_position, new_position, Gfx::Color::Black);

    m_most_recent_position = new_position;
}

void LassoSelectTool::on_mouseup(Layer*, MouseEvent&)
{
    if (!m_selecting)
        return;

    if (m_selection_bitmap.is_null())
        return;

    m_selecting = false;
    m_bottom_right.translate_by(1);

    if (m_most_recent_position != m_start_position) {
        auto selection_painter = Gfx::Painter(*m_selection_bitmap);
        selection_painter.draw_line(m_most_recent_position, m_start_position, Gfx::Color::Black, 1);
    }

    auto cropped_selection_result = m_selection_bitmap->cropped(Gfx::Rect<int>::from_two_points(m_top_left, m_bottom_right));
    if (cropped_selection_result.is_error())
        return;
    auto cropped_selection = cropped_selection_result.release_value();

    // We create a bitmap that is bigger by 1 pixel on each side
    auto lasso_bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { (m_bottom_right.x() - m_top_left.x()) + 2, (m_bottom_right.y() - m_top_left.y()) + 2 });
    if (lasso_bitmap_or_error.is_error())
        return;

    auto lasso_bitmap = lasso_bitmap_or_error.release_value();

    auto lasso_painter = Gfx::Painter(lasso_bitmap);

    // We want to paint the lasso into the bitmap such that there is an empty 1px border on each side
    // this ensures that we have a known pixel (0,0) that is outside the lasso.
    // Because we want a 1 px offset to the right and down, we blit the cropped selection bitmap starting at (1,1).
    lasso_painter.blit({ 1, 1 }, cropped_selection, cropped_selection->rect());

    // Delta to use for mapping the bitmap back to layer coordinates. -1 to account for the right and down offset.
    auto bitmap_to_layer_delta = Gfx::IntPoint(m_top_left.x() + m_editor->active_layer()->location().x() - 1, m_top_left.y() + m_editor->active_layer()->location().y() - 1);
    flood_lasso_selection(lasso_bitmap, bitmap_to_layer_delta);
}

void LassoSelectTool::flood_lasso_selection(Gfx::Bitmap& lasso_bitmap, Gfx::IntPoint lasso_delta)
{
    VERIFY(lasso_bitmap.bpp() == 32);

    // Create Mask which will track already-processed pixels
    Mask selection_mask = Mask::full(lasso_bitmap.rect().translated(lasso_delta));

    auto pixel_reached = [&](Gfx::IntPoint location) {
        selection_mask.set(Gfx::IntPoint(location.x(), location.y()).translated(lasso_delta), 0);
    };

    lasso_bitmap.flood_visit_from_point({ 0, 0 }, 0, move(pixel_reached));

    selection_mask.shrink_to_fit();
    selection_mask.bounding_rect().translate_by(m_editor->active_layer()->location());
    m_editor->image().selection().merge(selection_mask, m_merge_mode);
}

void LassoSelectTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (!m_selecting)
        return;
    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    if (layer)
        painter.translate(editor_layer_location(*layer));
    painter.stroke_path(m_preview_path, Gfx::Color::Black, 1);
}

bool LassoSelectTool::on_keydown(GUI::KeyEvent const& key_event)
{
    Tool::on_keydown(key_event);
    if (key_event.key() == KeyCode::Key_Escape) {
        if (m_selecting) {
            m_selecting = false;
            m_selection_bitmap.clear();
            m_preview_path.clear();
            return true;
        }
    }

    return Tool::on_keydown(key_event);
}

GUI::Widget* LassoSelectTool::get_properties_widget()
{
    if (m_properties_widget) {
        return m_properties_widget.ptr();
    }

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

}
