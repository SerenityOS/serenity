/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MouseWidget.h"

#include <Applications/MouseSettings/MouseWidgetGML.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/WindowServerConnection.h>
#include <WindowServer/WindowManager.h>

constexpr double speed_slider_scale = 100.0;
constexpr int default_scroll_length = 4;
constexpr int double_click_speed_default = 250;

MouseWidget::MouseWidget()
{
    load_from_gml(mouse_widget_gml);

    m_speed_label = *find_descendant_of_type_named<GUI::Label>("speed_label");
    m_speed_slider = *find_descendant_of_type_named<GUI::HorizontalSlider>("speed_slider");
    m_speed_slider->set_range(WindowServer::mouse_accel_min * speed_slider_scale, WindowServer::mouse_accel_max * speed_slider_scale);
    m_speed_slider->on_change = [&](int value) {
        m_speed_label->set_text(String::formatted("{} %", value));
    };
    int const slider_value = float { speed_slider_scale } * GUI::WindowServerConnection::the().get_mouse_acceleration();
    m_speed_slider->set_value(slider_value);

    auto& cursor_speed_image_label = *find_descendant_of_type_named<GUI::Label>("cursor_speed_image_label");
    cursor_speed_image_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/graphics/mouse-cursor-speed.png"));

    auto& scroll_step_size_image_label = *find_descendant_of_type_named<GUI::Label>("scroll_step_size_image_label");
    scroll_step_size_image_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/graphics/scroll-wheel-step-size.png"));

    m_scroll_length_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("scroll_length_spinbox");
    m_scroll_length_spinbox->set_min(WindowServer::scroll_step_size_min);
    m_scroll_length_spinbox->set_value(GUI::WindowServerConnection::the().get_scroll_step_size());

    m_double_click_arrow_widget = *find_descendant_of_type_named<MouseSettings::DoubleClickArrowWidget>("double_click_arrow_widget");
    m_double_click_speed_label = *find_descendant_of_type_named<GUI::Label>("double_click_speed_label");
    m_double_click_speed_slider = *find_descendant_of_type_named<GUI::HorizontalSlider>("double_click_speed_slider");
    m_double_click_speed_slider->set_min(WindowServer::double_click_speed_min);
    m_double_click_speed_slider->set_max(WindowServer::double_click_speed_max);
    m_double_click_speed_slider->on_change = [&](int speed) {
        m_double_click_arrow_widget->set_double_click_speed(speed);
        m_double_click_speed_label->set_text(String::formatted("{} ms", speed));
    };
    m_double_click_speed_slider->set_value(GUI::WindowServerConnection::the().get_double_click_speed());
}

void MouseWidget::update_window_server()
{
    float const factor = m_speed_slider->value() / speed_slider_scale;
    GUI::WindowServerConnection::the().async_set_mouse_acceleration(factor);
    GUI::WindowServerConnection::the().async_set_scroll_step_size(m_scroll_length_spinbox->value());
    GUI::WindowServerConnection::the().async_set_double_click_speed(m_double_click_speed_slider->value());
}

void MouseWidget::reset_default_values()
{
    m_speed_slider->set_value(speed_slider_scale);
    m_scroll_length_spinbox->set_value(default_scroll_length);
    m_double_click_speed_slider->set_value(double_click_speed_default);
    update_window_server();
}

MouseWidget::~MouseWidget()
{
}
