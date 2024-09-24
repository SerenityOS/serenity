/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageMasking.h"
#include <Applications/PixelPaint/ColorMaskingGML.h>
#include <Applications/PixelPaint/LuminosityMaskingGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RangeSlider.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>

namespace PixelPaint {

ImageMasking::ImageMasking(GUI::Window* parent_window, ImageEditor* editor, MaskingType masking_type)
    : GUI::Dialog(parent_window)
    , m_masking_type(masking_type)
    , m_editor(editor)
{
    set_icon(parent_window->icon());

    auto main_widget = set_main_widget<GUI::Widget>();

    set_resizable(false);
    m_previous_edit_mode = m_editor->active_layer()->edit_mode();
    m_editor->active_layer()->set_edit_mode(Layer::EditMode::Mask);

    if (m_masking_type == MaskingType::Luminosity) {
        main_widget->load_from_gml(luminosity_masking_gml).release_value_but_fixme_should_propagate_errors();
        set_title("Luminosity Mask");
        resize(300, 170);

        m_full_masking_slider = main_widget->find_descendant_of_type_named<GUI::RangeSlider>("full_masking");
        m_edge_masking_slider = main_widget->find_descendant_of_type_named<GUI::RangeSlider>("edge_masking");
        auto range_illustration_container = main_widget->find_descendant_of_type_named<GUI::Widget>("range_illustration");
        VERIFY(m_full_masking_slider);
        VERIFY(m_edge_masking_slider);
        VERIFY(range_illustration_container);

        m_full_masking_slider->set_gradient_color(Color(0, 0, 0, 255), Color(255, 255, 255, 255));
        m_edge_masking_slider->set_gradient_color(Color(0, 0, 0, 255), Color(255, 255, 255, 255));

        auto illustration_widget = range_illustration_container->try_add<RangeIllustrationWidget>(m_edge_masking_slider, m_full_masking_slider).release_value();
        illustration_widget->set_width(range_illustration_container->width());
        illustration_widget->set_height(range_illustration_container->height());

        // check that edges of full and edge masking are not intersecting, and refine the mask with the updated values
        m_full_masking_slider->on_range_change = [this, illustration_widget](int lower, int upper) {
            if (lower < m_edge_masking_slider->lower_range())
                m_full_masking_slider->set_lower_range(AK::max(lower, m_edge_masking_slider->lower_range()));
            if (upper > m_edge_masking_slider->upper_range())
                m_full_masking_slider->set_upper_range(AK::min(upper, m_edge_masking_slider->upper_range()));

            illustration_widget->update();
            generate_new_mask();
        };
        m_edge_masking_slider->on_range_change = [this, illustration_widget](int lower, int upper) {
            if (lower > m_full_masking_slider->lower_range())
                m_edge_masking_slider->set_lower_range(AK::min(lower, m_full_masking_slider->lower_range()));
            if (upper < m_full_masking_slider->upper_range())
                m_edge_masking_slider->set_upper_range(AK::max(upper, m_full_masking_slider->upper_range()));

            illustration_widget->update();
            generate_new_mask();
        };
    }

    if (m_masking_type == MaskingType::Color) {
        main_widget->load_from_gml(color_masking_gml).release_value_but_fixme_should_propagate_errors();

        set_title("Color Mask");
        resize(300, 250);

        m_saturation_value_masking_slider = main_widget->find_descendant_of_type_named<GUI::RangeSlider>("saturation_value");
        auto color_range_slider = main_widget->find_descendant_of_type_named<GUI::VerticalSlider>("color_range");
        auto hardness_slider = main_widget->find_descendant_of_type_named<GUI::VerticalSlider>("hardness");
        auto color_wheel_container = main_widget->find_descendant_of_type_named<GUI::Widget>("color_wheel_container");

        VERIFY(m_saturation_value_masking_slider);
        VERIFY(color_wheel_container);
        VERIFY(color_range_slider);
        VERIFY(hardness_slider);

        m_color_wheel_widget = color_wheel_container->try_add<ColorWheelWidget>().release_value();
        m_color_wheel_widget->set_width(color_wheel_container->width());
        m_color_wheel_widget->set_height(color_wheel_container->height());

        auto update_control_gradients = [this, color_range_slider, hardness_slider]() {
            auto selected_color = Gfx::Color::from_hsv(m_color_wheel_widget->hue(), 1, 1);
            m_saturation_value_masking_slider->set_gradient_colors(Vector {
                Gfx::ColorStop { Color(0, 0, 0, 255), 0 },
                Gfx::ColorStop { selected_color, 0.5 },
                Gfx::ColorStop { Color(255, 255, 255, 255), 1 } });
            color_range_slider->set_value(m_color_wheel_widget->color_range());
            hardness_slider->set_value(m_color_wheel_widget->hardness());
        };

        auto hsv = editor->primary_color().to_hsv();
        m_color_wheel_widget->set_hue(hsv.hue);
        m_color_wheel_widget->set_color_range(15);
        update_control_gradients();

        m_saturation_value_masking_slider->on_range_change = [this](int, int) {
            generate_new_mask();
        };

        color_range_slider->on_change = [this](int value) {
            m_color_wheel_widget->set_color_range(value);
        };

        hardness_slider->on_change = [this](int value) {
            m_color_wheel_widget->set_hardness(value);
        };

        m_color_wheel_widget->on_change = [this, update_control_gradients, color_range_slider, hardness_slider](double, double color_range, int hardness) {
            color_range_slider->set_value(color_range);
            hardness_slider->set_value(hardness);
            update_control_gradients();
            generate_new_mask();
        };
    }

    auto mask_visibility = main_widget->find_descendant_of_type_named<GUI::CheckBox>("mask_visibility");
    auto apply_button = main_widget->find_descendant_of_type_named<GUI::Button>("apply_button");
    auto cancel_button = main_widget->find_descendant_of_type_named<GUI::Button>("cancel_button");

    VERIFY(mask_visibility);
    VERIFY(apply_button);
    VERIFY(cancel_button);
    VERIFY(m_editor->active_layer());

    mask_visibility->set_checked(m_editor->active_layer()->mask_visibility());
    mask_visibility->on_checked = [this](auto checked) {
        m_editor->active_layer()->set_mask_visibility(checked);
        m_editor->update();
    };

    apply_button->on_click = [this](auto) {
        if (m_did_change)
            m_editor->did_complete_action("Image Masking"sv);

        done(ExecResult::OK);
    };

    cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };

    generate_new_mask();
}

void ImageMasking::generate_new_mask()
{
    ensure_reference_mask().release_value_but_fixme_should_propagate_errors();

    if (m_reference_mask.is_null() || !m_masked_area.has_value())
        return;

    if (m_masking_type == MaskingType::Luminosity) {
        int min_luminosity_start = m_edge_masking_slider->lower_range();
        int min_luminosity_full = m_full_masking_slider->lower_range();
        int max_luminosity_full = m_full_masking_slider->upper_range();
        int max_luminosity_end = m_edge_masking_slider->upper_range();
        int current_content_luminosity, approximation_alpha;
        bool has_start_range = min_luminosity_start != min_luminosity_full;
        bool has_end_range = max_luminosity_end != max_luminosity_full;
        Gfx::Color reference_mask_pixel, content_pixel;

        for (int y = m_masked_area->top(); y < m_masked_area->bottom(); y++) {
            for (int x = m_masked_area->left(); x < m_masked_area->right(); x++) {
                reference_mask_pixel = m_reference_mask->get_pixel(x, y);
                if (!reference_mask_pixel.alpha())
                    continue;

                content_pixel = m_editor->active_layer()->content_bitmap().get_pixel(x, y);
                current_content_luminosity = content_pixel.luminosity();

                if (!content_pixel.alpha() || current_content_luminosity < min_luminosity_start || current_content_luminosity > max_luminosity_end) {
                    reference_mask_pixel.set_alpha(0);
                } else if (current_content_luminosity >= min_luminosity_start && current_content_luminosity < min_luminosity_full && has_start_range) {
                    approximation_alpha = reference_mask_pixel.alpha() * static_cast<float>((current_content_luminosity - min_luminosity_start)) / (min_luminosity_full - min_luminosity_start);
                    reference_mask_pixel.set_alpha(approximation_alpha);
                } else if (current_content_luminosity > max_luminosity_full && current_content_luminosity <= max_luminosity_end && has_end_range) {
                    approximation_alpha = reference_mask_pixel.alpha() * (1 - static_cast<float>((current_content_luminosity - max_luminosity_full)) / (max_luminosity_end - max_luminosity_full));
                    reference_mask_pixel.set_alpha(approximation_alpha);
                }

                m_editor->active_layer()->mask_bitmap()->set_pixel(x, y, reference_mask_pixel);
            }
        }
    }

    if (m_masking_type == MaskingType::Color) {
        double lower_saturation = 1;
        double upper_saturation = 1;
        double lower_value = 1;
        double upper_value = 1;

        // m_saturation_value_masking_slider value description:
        // - saturation part in the positive range
        // - value part in the negative range
        if (m_saturation_value_masking_slider->upper_range() <= 0) {
            lower_value = (100 + m_saturation_value_masking_slider->lower_range()) / 100.0;
            upper_value = (100 + m_saturation_value_masking_slider->upper_range()) / 100.0;
        } else if (m_saturation_value_masking_slider->lower_range() >= 0) {
            lower_saturation = 1.0 - (m_saturation_value_masking_slider->upper_range() / 100.0);
            upper_saturation = 1.0 - (m_saturation_value_masking_slider->lower_range() / 100.0);
        } else {
            lower_value = (100 + m_saturation_value_masking_slider->lower_range()) / 100.0;
            upper_value = 1.0;
            lower_saturation = 1.0 - m_saturation_value_masking_slider->upper_range() / 100.0;
            upper_saturation = 1;
        }

        double full_masking_edge = m_color_wheel_widget->hardness();
        double gradient_masking_length = m_color_wheel_widget->color_range() - m_color_wheel_widget->hardness();
        double corrected_current_hue;
        double distance_to_selected_color = 0;
        Gfx::Color reference_mask_pixel;
        Gfx::HSV content_pixel_hsv;

        for (int y = m_masked_area->top(); y < m_masked_area->bottom(); y++) {
            auto reference_scanline = m_reference_mask->scanline(y);
            auto content_scanline = m_editor->active_layer()->content_bitmap().scanline(y);
            auto mask_scanline = m_editor->active_layer()->mask_bitmap()->scanline(y);
            fast_u32_fill(mask_scanline, 0, m_reference_mask->physical_width());

            for (int x = m_masked_area->left(); x < m_masked_area->right(); x++) {
                reference_mask_pixel = Color::from_argb(reference_scanline[x]);
                if (!reference_mask_pixel.alpha())
                    continue;

                content_pixel_hsv = Color::from_argb(content_scanline[x]).to_hsv();

                // check against saturation
                if (!(lower_saturation <= content_pixel_hsv.saturation && upper_saturation >= content_pixel_hsv.saturation))
                    continue;

                // check against value
                if (!(lower_value <= content_pixel_hsv.value && upper_value >= content_pixel_hsv.value))
                    continue;

                // check against hue
                corrected_current_hue = content_pixel_hsv.hue - m_color_wheel_widget->hue();
                distance_to_selected_color = AK::min(AK::abs(corrected_current_hue), AK::min(AK::abs(corrected_current_hue - 360), AK::abs(corrected_current_hue + 360)));
                if (distance_to_selected_color > m_color_wheel_widget->color_range())
                    continue;

                if (distance_to_selected_color < full_masking_edge) {
                    mask_scanline[x] = reference_mask_pixel.value();
                    continue;
                }

                mask_scanline[x] = reference_mask_pixel.with_alpha(reference_mask_pixel.alpha() - (((distance_to_selected_color - full_masking_edge) * reference_mask_pixel.alpha()) / gradient_masking_length)).value();
            }
        }
    }

    m_editor->active_layer()->did_modify_bitmap();
    m_did_change = true;
}

ErrorOr<void> ImageMasking::ensure_reference_mask()
{
    if (m_reference_mask.is_null()) {
        m_reference_mask = TRY(m_editor->active_layer()->mask_bitmap()->clone());
        m_masked_area = m_editor->active_layer()->editing_mask_bounding_rect();
        if (!m_masked_area.has_value())
            GUI::MessageBox::show(this, "You have to draw a mask first before you can refine the mask details."sv, "Missing mask content"sv, GUI::MessageBox::Type::Information);
    }
    return {};
}

void ImageMasking::on_done(GUI::Dialog::ExecResult result)
{
    if (result != GUI::Dialog::ExecResult::OK && m_did_change && m_reference_mask)
        m_editor->active_layer()->set_bitmaps(m_editor->active_layer()->content_bitmap(), m_reference_mask.release_nonnull()).release_value_but_fixme_should_propagate_errors();

    if (m_reference_mask)
        m_reference_mask = nullptr;

    m_editor->active_layer()->set_edit_mode(m_previous_edit_mode);
}

void RangeIllustrationWidget::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);
    painter.fill_rect(Gfx::IntRect(0, 0, width(), height()), palette().color(background_role()));
    float fraction = width() / 255.0f;

    Gfx::Path illustration;
    illustration.move_to({ fraction * m_edge_mask_values->lower_range(), static_cast<float>(height()) });
    illustration.line_to({ fraction * m_full_mask_values->lower_range(), 0 });
    illustration.line_to({ fraction * m_full_mask_values->upper_range(), 0 });
    illustration.line_to({ fraction * m_edge_mask_values->upper_range(), static_cast<float>(height()) });
    illustration.close();

    painter.fill_path(illustration, Color::MidGray);
}

void ColorWheelWidget::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);
    painter.save();

    auto wedge_edge = Gfx::FloatPoint(0, -height() / 2);

    float deg_as_radians = AK::to_radians(10.0f);
    Gfx::AffineTransform transform;
    transform.rotate_radians(deg_as_radians);

    painter.translate(width() / 2, height() / 2);

    for (int deg = 0; deg < 360; deg += 10) {
        auto rotated_edge = wedge_edge.transformed(transform);
        Gfx::Path wedge;
        wedge.move_to({
            0,
            0,
        });
        wedge.line_to(wedge_edge);
        wedge.line_to(rotated_edge);
        wedge.line_to({
            0,
            0,
        });
        wedge.close();

        painter.fill_path(wedge, Color::from_hsv(deg, 1, 1));

        wedge_edge = rotated_edge;
    }

    transform.rotate_radians(-deg_as_radians);
    deg_as_radians = AK::to_radians(static_cast<float>(hue()));
    transform.rotate_radians(deg_as_radians);
    auto selected_color = Gfx::FloatPoint(0, -height() / 2);
    selected_color.transform_by(transform);

    deg_as_radians = AK::to_radians(static_cast<float>(color_range()));

    auto selected_color_edge_1 = Gfx::FloatPoint(0, -height() / 2);
    transform.rotate_radians(deg_as_radians);
    selected_color_edge_1.transform_by(transform);

    auto selected_color_edge_2 = Gfx::FloatPoint(0, -height() / 2);
    transform.rotate_radians(-deg_as_radians);
    transform.rotate_radians(-deg_as_radians);
    selected_color_edge_2.transform_by(transform);

    transform.rotate_radians(deg_as_radians);
    deg_as_radians = AK::to_radians(static_cast<float>(color_range() * static_cast<double>(hardness()) / 100.0));

    auto hardness_edge_1 = Gfx::FloatPoint(0, -height() / 2);
    transform.rotate_radians(deg_as_radians);
    hardness_edge_1.transform_by(transform);

    auto hardness_edge_2 = Gfx::FloatPoint(0, -height() / 2);
    transform.rotate_radians(-deg_as_radians);
    transform.rotate_radians(-deg_as_radians);
    hardness_edge_2.transform_by(transform);

    Gfx::AntiAliasingPainter aa_painter = Gfx::AntiAliasingPainter(painter);

    aa_painter.draw_line(Gfx::IntPoint(0, 0), selected_color_edge_1.to_type<int>(), Color::White, 2);
    aa_painter.draw_line(Gfx::IntPoint(0, 0), selected_color_edge_2.to_type<int>(), Color::White, 2);
    aa_painter.draw_line(Gfx::IntPoint(0, 0), hardness_edge_1.to_type<int>(), Color::LightGray, 1);
    aa_painter.draw_line(Gfx::IntPoint(0, 0), hardness_edge_2.to_type<int>(), Color::LightGray, 1);
    aa_painter.draw_line(Gfx::IntPoint(0, 0), selected_color.to_type<int>(), Color::Black, 3);
    aa_painter.fill_circle({ 0, 0 }, height() / 4, Color(Color::LightGray));
    aa_painter.fill_circle({ 0, 0 }, (height() - 4) / 4, Color::from_hsv(hue(), 1, 1));

    painter.restore();
    auto hue_text = ByteString::formatted("hue: {:.0}", hue());
    painter.draw_text(rect().translated(1, 1), hue_text, Gfx::TextAlignment::Center, Color::Black);
    painter.draw_text(rect(), hue_text, Gfx::TextAlignment::Center, Color::White);
}

void ColorWheelWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary)
        m_mouse_pressed = true;
}

void ColorWheelWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (m_mouse_pressed)
        calc_hue(event.position());
    m_mouse_pressed = false;
}

void ColorWheelWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_mouse_pressed)
        return;

    calc_hue(event.position());
}

void ColorWheelWidget::mousewheel_event(GUI::MouseEvent& event)
{
    if (event.ctrl())
        set_color_range(color_range() + event.wheel_delta_y());
    else if (event.shift())
        set_hardness(hardness() + event.wheel_delta_y());
    else
        set_hue(hue() + event.wheel_delta_y());
}

void ColorWheelWidget::set_hue(double value)
{
    if (value < 0)
        value += 360.0;

    value = AK::fmod(value, 360.0);
    if (m_hue != value) {
        m_hue = value;
        update();

        if (on_change)
            on_change(hue(), color_range(), hardness());
    }
}

double ColorWheelWidget::hue()
{
    return m_hue;
}

void ColorWheelWidget::calc_hue(Gfx::IntPoint const& position)
{
    auto center = Gfx::IntPoint(width() / 2, height() / 2);
    auto angle = AK::to_degrees(AK::atan2(static_cast<float>(position.y() - center.y()), static_cast<float>(position.x() - center.x())));

    set_hue(angle + 90);
}

double ColorWheelWidget::color_range()
{
    return m_color_range;
}

void ColorWheelWidget::set_color_range(double value)
{
    value = clamp(value, 0.0, 180.0);
    if (m_color_range != value) {
        m_color_range = value;
        update();

        if (on_change)
            on_change(hue(), color_range(), hardness());
    }
}

void ColorWheelWidget::set_hardness(int value)
{
    value = clamp(value, 0, 100);
    if (m_hardness != value) {
        m_hardness = value;
        update();

        if (on_change)
            on_change(hue(), color_range(), hardness());
    }
}

int ColorWheelWidget::hardness()
{
    return m_hardness;
}

}
