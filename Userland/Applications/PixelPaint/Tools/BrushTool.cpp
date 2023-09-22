/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022-2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrushTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

void BrushTool::set_size(int size)
{
    if (size == m_size)
        return;
    m_size = size;
    refresh_editor_cursor();
    auto may_have_failed = ensure_brush_reference_bitmap(m_ensured_color);
    if (may_have_failed.is_error())
        GUI::MessageBox::show_error(nullptr, MUST(String::formatted("Failed to create the brush. error: {}", may_have_failed.release_error())));
}

void BrushTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary && layer_event.button() != GUI::MouseButton::Secondary)
        return;

    // Shift+Click draws a line from the last position to current one.
    if (layer_event.shift() && m_has_clicked) {
        draw_line(layer->get_scratch_edited_bitmap(), color_for(layer_event), m_last_position, layer_event.position());
        auto modified_rect = Gfx::IntRect::from_two_points(m_last_position, layer_event.position()).inflated(m_size * 2, m_size * 2);
        layer->did_modify_bitmap(modified_rect);
        m_last_position = layer_event.position();
        return;
    }

    draw_point(layer->get_scratch_edited_bitmap(), color_for(layer_event), layer_event.position());

    layer->did_modify_bitmap(Gfx::IntRect::centered_on(layer_event.position(), Gfx::IntSize { m_size * 2, m_size * 2 }));
    m_last_position = layer_event.position();
    m_has_clicked = true;
    m_was_drawing = true;
}

void BrushTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (!(layer_event.buttons() & GUI::MouseButton::Primary || layer_event.buttons() & GUI::MouseButton::Secondary))
        return;

    draw_line(layer->get_scratch_edited_bitmap(), color_for(layer_event), m_last_position, layer_event.position());

    auto modified_rect = Gfx::IntRect::from_two_points(m_last_position, layer_event.position()).inflated(m_size * 2, m_size * 2);

    layer->did_modify_bitmap(modified_rect);
    m_last_position = layer_event.position();
    m_was_drawing = true;
}

void BrushTool::on_mouseup(Layer*, MouseEvent&)
{
    m_is_drawing_line = false;
    m_last_draw_rect = {};

    if (m_was_drawing) {
        m_editor->did_complete_action(tool_name());
        m_was_drawing = false;
    }
}

Color BrushTool::color_for(GUI::MouseEvent const& event)
{
    return m_editor->color_for(event);
}

void BrushTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint point)
{
    if (ensure_brush_reference_bitmap(color).is_error())
        return;

    if ((m_mode == BrushMode::Normal && m_editor->active_layer()->mask_type() != Layer::MaskType::EditingMask) || m_editor->active_layer()->edit_mode() == Layer::EditMode::Mask) {
        Gfx::Painter painter = Gfx::Painter(bitmap);
        painter.blit(point.translated(-size()), *m_brush_reference, m_brush_reference->rect());
        return;
    }

    auto current_draw_rect = Gfx::IntRect(point.x() - size(), point.y() - size(), size() * 2, size() * 2);

    if (current_draw_rect.location() == m_last_draw_rect.location())
        return;

    auto intersection = Gfx::IntRect::intersection(m_last_draw_rect, current_draw_rect);
    auto offset_current_to_last_draw = current_draw_rect.location() - m_last_draw_rect.location();
    int last_reference_x, last_reference_y, reference_x, reference_y;
    Gfx::Color brush_color_used, last_drawn_pixel, bitmap_color;

    for (int y = point.y() - size(); y < point.y() + size(); y++) {
        for (int x = point.x() - size(); x < point.x() + size(); x++) {
            if (x < 0 || x >= bitmap.width() || y < 0 || y >= bitmap.height())
                continue;

            reference_x = x - point.x() + size();
            reference_y = y - point.y() + size();

            if (reference_x < 0 || reference_y < 0)
                continue;

            brush_color_used = m_brush_reference->get_pixel<Gfx::StorageFormat::BGRA8888>(reference_x, reference_y);
            if (!brush_color_used.alpha())
                continue;

            if (m_mode != BrushMode::Normal && intersection.contains({ x, y })) {
                last_reference_x = reference_x + offset_current_to_last_draw.x();
                last_reference_y = reference_y + offset_current_to_last_draw.y();
                last_drawn_pixel = m_brush_reference->get_pixel<Gfx::StorageFormat::BGRA8888>(last_reference_x, last_reference_y);

                if (last_drawn_pixel.alpha() < brush_color_used.alpha())
                    brush_color_used.set_alpha(brush_color_used.alpha() - last_drawn_pixel.alpha());
                else
                    continue;
            }

            if (m_mode == BrushMode::Dodge || m_mode == BrushMode::Burn) {
                bitmap_color = bitmap.get_pixel(x, y);

                if (!bitmap_color.alpha())
                    continue;

                brush_color_used = Gfx::Color(
                    m_precomputed_color_values[bitmap_color.red()],
                    m_precomputed_color_values[bitmap_color.green()],
                    m_precomputed_color_values[bitmap_color.blue()],
                    (static_cast<float>(bitmap_color.alpha()) / 255) * brush_color_used.alpha());
            }

            set_pixel_with_possible_mask(x, y, bitmap.get_pixel(x, y).blend(brush_color_used), bitmap);
        }
    }

    m_last_draw_rect = current_draw_rect;
}

void BrushTool::draw_line(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint start, Gfx::IntPoint end)
{
    int length_x = end.x() - start.x();
    int length_y = end.y() - start.y();
    float y_step = length_y == 0 ? 0 : (float)(length_y) / (float)(length_x);
    if (y_step > abs(length_y))
        y_step = abs(length_y);
    if (y_step < -abs(length_y))
        y_step = -abs(length_y);
    if (y_step == 0 && start.x() == end.x())
        return;

    int start_x = start.x();
    int end_x = end.x();
    int start_y = start.y();
    int end_y = end.y();
    if (start_x > end_x) {
        swap(start_x, end_x);
        swap(start_y, end_y);
    }
    if (!m_is_drawing_line)
        m_last_draw_rect = {};
    m_is_drawing_line = true;

    float y = start_y;
    for (int x = start_x; x <= end_x; x++) {
        int start_step_y = y;
        int end_step_y = y + y_step;
        if (start_step_y > end_step_y)
            swap(start_step_y, end_step_y);
        for (int i = start_step_y; i <= end_step_y; i++)
            draw_point(bitmap, color, { x, i });
        y += y_step;
    }
}

NonnullRefPtr<GUI::Widget> BrushTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = GUI::Widget::construct();
        properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& mode_container = properties_widget->add<GUI::Widget>();
        mode_container.set_fixed_height(20);
        mode_container.set_layout<GUI::HorizontalBoxLayout>();
        auto& mode_label = mode_container.add<GUI::Label>("Mode:"_string);
        mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        mode_label.set_fixed_size(60, 20);

        static constexpr auto s_mode_names = [] {
            Array<StringView, (int)BrushMode::__Count> names;
            for (size_t i = 0; i < names.size(); i++) {
                switch ((BrushMode)i) {
                case BrushMode::Normal:
                    names[i] = "Normal"sv;
                    break;
                case BrushMode::Soft:
                    names[i] = "Soft"sv;
                    break;
                case BrushMode::Burn:
                    names[i] = "Burn"sv;
                    break;
                case BrushMode::Dodge:
                    names[i] = "Dodge"sv;
                    break;
                default:
                    break;
                }
            }
            return names;
        }();

        auto& mode_combobox = mode_container.add<GUI::ComboBox>();
        mode_combobox.set_only_allow_values_from_model(true);
        mode_combobox.set_model(*GUI::ItemListModel<StringView, decltype(s_mode_names)>::create(s_mode_names));
        mode_combobox.set_selected_index((int)m_mode, GUI::AllowCallback::No);

        auto& priority_container = properties_widget->add<GUI::Widget>();
        priority_container.set_fixed_height(20);
        priority_container.set_visible(false);
        priority_container.set_layout<GUI::HorizontalBoxLayout>();
        auto& priority_label = priority_container.add<GUI::Label>("Priority:"_string);
        priority_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        priority_label.set_fixed_size(60, 20);

        static constexpr auto s_priority_names = [] {
            Array<StringView, (int)PriorityMode::__Count> names;
            for (size_t i = 0; i < names.size(); i++) {
                switch ((PriorityMode)i) {
                case PriorityMode::Highlights:
                    names[i] = "Highlights"sv;
                    break;
                case PriorityMode::Midtones:
                    names[i] = "Midtones"sv;
                    break;
                case PriorityMode::Shadows:
                    names[i] = "Shadows"sv;
                    break;
                default:
                    break;
                }
            }
            return names;
        }();

        auto& priority_combobox = priority_container.add<GUI::ComboBox>();
        priority_combobox.set_only_allow_values_from_model(true);
        priority_combobox.set_model(*GUI::ItemListModel<StringView, decltype(s_priority_names)>::create(s_priority_names));
        priority_combobox.set_selected_index((int)m_priority, GUI::AllowCallback::No);

        auto& exposure_container = properties_widget->add<GUI::Widget>();
        exposure_container.set_fixed_height(20);
        exposure_container.set_visible(false);
        exposure_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& exposure_label = exposure_container.add<GUI::Label>("Exposure:"_string);
        exposure_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        exposure_label.set_fixed_size(60, 20);

        auto& exposure_slider = exposure_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%"_string);
        exposure_slider.set_range(1, 100);
        exposure_slider.set_value(m_exposure * 100);

        mode_combobox.on_change = [this, &priority_container, &exposure_container](auto&, auto& model_index) {
            VERIFY(model_index.row() >= 0);
            VERIFY(model_index.row() < (int)BrushMode::__Count);

            m_mode = (BrushMode)model_index.row();
            priority_container.set_visible(m_mode == BrushMode::Dodge || m_mode == BrushMode::Burn);
            exposure_container.set_visible(m_mode == BrushMode::Dodge || m_mode == BrushMode::Burn);

            if (m_mode == BrushMode::Dodge || m_mode == BrushMode::Burn)
                update_precomputed_color_values();
        };

        priority_combobox.on_change = [this](auto&, auto& model_index) {
            VERIFY(model_index.row() >= 0);
            VERIFY(model_index.row() < (int)PriorityMode::__Count);

            m_priority = (PriorityMode)model_index.row();
            update_precomputed_color_values();
        };

        exposure_slider.on_change = [this](int value) {
            m_exposure = value / 100.0f;
            update_precomputed_color_values();
        };

        auto& size_container = properties_widget->add<GUI::Widget>();
        size_container.set_fixed_height(20);
        size_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& size_label = size_container.add<GUI::Label>("Size:"_string);
        size_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        size_label.set_fixed_size(60, 20);

        auto& size_slider = size_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px"_string);
        size_slider.set_range(1, 250);
        size_slider.set_value(m_size);
        size_slider.set_override_cursor(cursor());

        size_slider.on_change = [this, &size_slider](int value) {
            set_size(value);
            // Update cursor to provide an instant preview for the selected size.
            size_slider.set_override_cursor(cursor());
        };
        set_primary_slider(&size_slider);

        auto& hardness_container = properties_widget->add<GUI::Widget>();
        hardness_container.set_fixed_height(20);
        hardness_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& hardness_label = hardness_container.add<GUI::Label>("Hardness:"_string);
        hardness_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        hardness_label.set_fixed_size(60, 20);

        auto& hardness_slider = hardness_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%"_string);
        hardness_slider.set_range(1, 100);
        hardness_slider.set_value(m_hardness);

        hardness_slider.on_change = [this](int value) {
            set_hardness(value);
        };
        set_secondary_slider(&hardness_slider);
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

NonnullRefPtr<Gfx::Bitmap> BrushTool::build_cursor()
{
    m_scale_last_created_cursor = m_editor ? m_editor->scale() : 1;
    auto containing_box_size = AK::clamp(preferred_cursor_size(), 1.0f, max_allowed_cursor_size());
    auto centered = containing_box_size / 2;
    NonnullRefPtr<Gfx::Bitmap> new_cursor = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, Gfx::IntSize(containing_box_size, containing_box_size)).release_value_but_fixme_should_propagate_errors();

    Gfx::Painter painter { new_cursor };
    Gfx::AntiAliasingPainter aa_painter { painter };

    painter.draw_line({ centered - 5, centered }, { centered + 5, centered }, Color::LightGray, 3);
    painter.draw_line({ centered, centered - 5 }, { centered, centered + 5 }, Color::LightGray, 3);
    painter.draw_line({ centered - 5, centered }, { centered + 5, centered }, Color::MidGray, 1);
    painter.draw_line({ centered, centered - 5 }, { centered, centered + 5 }, Color::MidGray, 1);
    if (max_allowed_cursor_size() != containing_box_size) {
        aa_painter.draw_ellipse(Gfx::IntRect(0, 0, containing_box_size, containing_box_size), Color::LightGray, 1);
    } else {
        aa_painter.draw_ellipse(Gfx::IntRect(0, 0, containing_box_size, containing_box_size), Color::Red, 1);
        aa_painter.draw_ellipse(Gfx::IntRect(3, 3, containing_box_size - 6, containing_box_size - 6), Color::LightGray, 1);
    }
    return new_cursor;
}

void BrushTool::refresh_editor_cursor()
{
    m_cursor = build_cursor();
    if (m_editor)
        m_editor->update_tool_cursor();
}

ErrorOr<void> BrushTool::ensure_brush_reference_bitmap(Gfx::Color color)
{
    Gfx::IntSize brush_size = Gfx::IntSize(size() * 2, size() * 2);

    if (m_brush_reference.is_null() || m_brush_reference->size() != brush_size)
        m_brush_reference = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, brush_size));
    else if (m_ensured_color != color || m_ensured_hardness != hardness())
        m_brush_reference->fill(Color::Transparent);
    else
        return {};

    m_ensured_color = color;
    m_ensured_hardness = hardness();
    constexpr auto flow_scale = 10;
    Gfx::IntPoint center_point = { size(), size() };
    for (int y = 0; y < m_brush_reference->height(); y++) {
        for (int x = 0; x < m_brush_reference->width(); x++) {
            auto distance = center_point.distance_from({ x, y });
            if (distance >= size())
                continue;

            auto falloff = get_falloff(distance) * flow_scale;
            auto pixel_color = color;
            pixel_color.set_alpha(AK::min(falloff * 255, 255));
            m_brush_reference->set_pixel(x, y, pixel_color);
        }
    }
    return {};
}

float BrushTool::preferred_cursor_size()
{
    return 2 * size() * (m_editor ? m_editor->scale() : 1);
}

float BrushTool::max_allowed_cursor_size()
{
    return m_editor ? Gfx::IntPoint(0, 0).distance_from({ m_editor->width(), m_editor->height() }) * 1.1f : 500;
}

void BrushTool::update_precomputed_color_values()
{
    float dodge_burn_factor = 0;
    switch (m_priority) {
    case PriorityMode::Highlights:
        if (m_mode == BrushMode::Dodge)
            dodge_burn_factor = 1.0f + (m_exposure / 3.0f);
        else
            dodge_burn_factor = 1.0f - (m_exposure / 3.0f);
        break;
    case PriorityMode::Midtones:
        if (m_mode == BrushMode::Dodge)
            dodge_burn_factor = 1.0f / (1.0f + m_exposure);
        else
            dodge_burn_factor = 1.0f / (1.0f - m_exposure);
        break;
    case PriorityMode::Shadows:
        dodge_burn_factor = (1.0f - m_exposure * 0.5f);
        break;
    case PriorityMode::__Count:
        VERIFY_NOT_REACHED();
        break;
    }

    float scaled_color = 0;
    for (int color_val = 0; color_val < 256; color_val++) {
        scaled_color = static_cast<float>(color_val) / 255.0f;
        if (m_priority == PriorityMode::Highlights)
            m_precomputed_color_values[color_val] = AK::min(color_val * dodge_burn_factor, 255);

        if (m_priority == PriorityMode::Midtones)
            m_precomputed_color_values[color_val] = AK::min(AK::pow(static_cast<float>(scaled_color), dodge_burn_factor) * 255, 255);

        if (m_priority == PriorityMode::Shadows) {
            if (m_mode == BrushMode::Dodge)
                m_precomputed_color_values[color_val] = AK::min((dodge_burn_factor * scaled_color + (1.0f - dodge_burn_factor)) * 255, 255);
            else
                m_precomputed_color_values[color_val] = AK::max((scaled_color + m_exposure * (1.0f - AK::exp(1.0f - scaled_color))) * 255, 0);
        }
    }
}
}
