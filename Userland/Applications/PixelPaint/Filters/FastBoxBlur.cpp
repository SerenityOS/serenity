/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FastBoxBlur.h"
#include "../FilterParams.h"
#include <AK/ExtraMathConstants.h>
#include <LibGUI/Label.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Filters/FastBoxBlurFilter.h>

namespace PixelPaint::Filters {

void FastBoxBlur::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    // This filter only works in-place, so if we have different target and source, we first copy over
    // the source bitmap to the target one.
    if (&target_bitmap != &source_bitmap) {
        VERIFY(source_bitmap.size_in_bytes() == target_bitmap.size_in_bytes());
        memcpy(target_bitmap.scanline(0), source_bitmap.scanline(0), source_bitmap.size_in_bytes());
    }

    Gfx::FastBoxBlurFilter filter(target_bitmap);

    if (m_use_asymmetric_radii) {
        if (m_use_vector) {
            auto radius_x = m_radius * fabs(cos((double)m_angle * M_DEG2RAD));
            auto radius_y = m_radius * fabs(sin((double)m_angle * M_DEG2RAD));
            filter.apply_single_pass(radius_x, radius_y);
            return;
        }

        filter.apply_single_pass(m_radius_x, m_radius_y);
        return;
    }

    if (m_approximate_gauss)
        filter.apply_three_passes(m_radius);
    else
        filter.apply_single_pass(m_radius);
}

RefPtr<GUI::Widget> FastBoxBlur::get_settings_widget()
{
    if (!m_settings_widget) {
        m_settings_widget = GUI::Widget::construct();
        m_settings_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& name_label = m_settings_widget->add<GUI::Label>("Fast Box Blur Filter");
        name_label.set_font_weight(Gfx::FontWeight::Bold);
        name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        name_label.set_fixed_height(10);

        auto& asymmetric_checkbox = m_settings_widget->add<GUI::CheckBox>("Use Asymmetric Radii");
        asymmetric_checkbox.set_checked(false);
        asymmetric_checkbox.set_fixed_height(15);
        asymmetric_checkbox.on_checked = [&](bool checked) {
            m_use_asymmetric_radii = checked;
            if (m_use_asymmetric_radii) {
                m_vector_checkbox->set_visible(true);
                m_radius_x_slider->set_value(m_radius);
                m_radius_y_slider->set_value(m_radius);
                m_asymmetric_radius_container->set_visible(true);
                m_radius_container->set_visible(false);
                m_gaussian_checkbox->set_visible(false);
            } else {
                m_asymmetric_radius_container->set_visible(false);
                m_radius_container->set_visible(true);
                m_gaussian_checkbox->set_visible(true);
                m_vector_checkbox->set_visible(false);
            }
            update_preview();
        };

        m_vector_checkbox = m_settings_widget->add<GUI::CheckBox>("Use Direction and magnitude");
        m_vector_checkbox->set_checked(false);
        m_vector_checkbox->set_visible(false);
        m_vector_checkbox->set_fixed_height(15);
        m_vector_checkbox->on_checked = [&](bool checked) {
            m_use_vector = checked;
            if (m_use_vector) {
                m_asymmetric_radius_container->set_visible(false);
                m_vector_container->set_visible(true);
            } else {
                m_asymmetric_radius_container->set_visible(true);
                m_vector_container->set_visible(false);
            }
            update_preview();
        };

        m_radius_container = m_settings_widget->add<GUI::Widget>();
        m_radius_container->set_fixed_height(20);
        m_radius_container->set_layout<GUI::HorizontalBoxLayout>();
        m_radius_container->layout()->set_margins({ 4, 0, 4, 0 });

        auto& radius_label = m_radius_container->add<GUI::Label>("Radius:");
        radius_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        radius_label.set_fixed_size(50, 20);

        auto& radius_slider = m_radius_container->add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        radius_slider.set_range(0, 25);
        radius_slider.set_value(m_radius);
        radius_slider.on_change = [&](int value) {
            m_radius = value;
            update_preview();
        };

        m_asymmetric_radius_container = m_settings_widget->add<GUI::Widget>();
        m_asymmetric_radius_container->set_visible(false);
        m_asymmetric_radius_container->set_fixed_height(50);
        m_asymmetric_radius_container->set_layout<GUI::VerticalBoxLayout>();
        m_asymmetric_radius_container->layout()->set_margins({ 4, 0, 4, 0 });

        auto& radius_x_container = m_asymmetric_radius_container->add<GUI::Widget>();
        radius_x_container.set_fixed_height(20);
        radius_x_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& radius_x_label = radius_x_container.add<GUI::Label>("Radius X:");
        radius_x_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        radius_x_label.set_fixed_size(50, 20);

        m_radius_x_slider = radius_x_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        m_radius_x_slider->set_range(0, 50);
        m_radius_x_slider->set_value(m_radius_x);
        m_radius_x_slider->on_change = [&](int value) {
            m_radius_x = value;
            update_preview();
        };

        auto& radius_y_container = m_asymmetric_radius_container->add<GUI::Widget>();
        radius_y_container.set_fixed_height(20);
        radius_y_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& radius_y_label = radius_y_container.add<GUI::Label>("Radius Y:");
        radius_y_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        radius_y_label.set_fixed_size(50, 20);

        m_radius_y_slider = radius_y_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        m_radius_y_slider->set_range(0, 50);
        m_radius_y_slider->set_value(m_radius_y);
        m_radius_y_slider->on_change = [&](int value) {
            m_radius_y = value;
            update_preview();
        };

        m_vector_container = m_settings_widget->add<GUI::Widget>();
        m_vector_container->set_visible(false);
        m_vector_container->set_fixed_height(50);
        m_vector_container->set_layout<GUI::VerticalBoxLayout>();
        m_vector_container->layout()->set_margins({ 4, 0, 4, 0 });

        auto& angle_container = m_vector_container->add<GUI::Widget>();
        angle_container.set_fixed_height(20);
        angle_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& angle_label = angle_container.add<GUI::Label>("Angle:");
        angle_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        angle_label.set_fixed_size(60, 20);

        m_angle_slider = angle_container.add<GUI::ValueSlider>(Orientation::Horizontal, "°");
        m_angle_slider->set_range(0, 360);
        m_angle_slider->set_value(m_angle);
        m_angle_slider->on_change = [&](int value) {
            m_angle = value;
            update_preview();
        };

        auto& magnitude_container = m_vector_container->add<GUI::Widget>();
        magnitude_container.set_fixed_height(20);
        magnitude_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& magnitude_label = magnitude_container.add<GUI::Label>("Magnitude:");
        magnitude_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        magnitude_label.set_fixed_size(60, 20);

        m_magnitude_slider = magnitude_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        m_magnitude_slider->set_range(0, 50);
        m_magnitude_slider->set_value(m_radius);
        m_magnitude_slider->on_change = [&](int value) {
            m_radius = value;
            update_preview();
        };

        auto& gaussian_container = m_settings_widget->add<GUI::Widget>();
        gaussian_container.set_fixed_height(20);
        gaussian_container.set_layout<GUI::HorizontalBoxLayout>();
        gaussian_container.layout()->set_margins({ 4, 0, 4, 0 });

        m_gaussian_checkbox = gaussian_container.add<GUI::CheckBox>("Approximate Gaussian Blur");
        m_gaussian_checkbox->set_checked(m_approximate_gauss);
        m_gaussian_checkbox->set_tooltip("A real gaussian blur can be approximated by running the box blur multiple times with different weights.");
        m_gaussian_checkbox->on_checked = [this](bool checked) {
            m_approximate_gauss = checked;
            update_preview();
        };
    }

    return m_settings_widget;
}
}
