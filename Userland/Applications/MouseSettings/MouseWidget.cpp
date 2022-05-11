/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MouseWidget.h"

#include <Applications/MouseSettings/MouseWidgetGML.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <WindowServer/Screen.h>
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
    int const slider_value = float { speed_slider_scale } * GUI::ConnectionToWindowServer::the().get_mouse_acceleration();
    m_speed_slider->set_value(slider_value, GUI::AllowCallback::No);
    m_speed_slider->on_change = [&](int) {
        update_speed_label();
        set_modified(true);
    };

    m_scroll_length_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("scroll_length_spinbox");
    m_scroll_length_spinbox->set_min(WindowServer::scroll_step_size_min);
    m_scroll_length_spinbox->set_value(GUI::ConnectionToWindowServer::the().get_scroll_step_size(), GUI::AllowCallback::No);
    m_scroll_length_spinbox->on_change = [&](auto) {
        set_modified(true);
    };

    m_double_click_arrow_widget = *find_descendant_of_type_named<MouseSettings::DoubleClickArrowWidget>("double_click_arrow_widget");
    m_double_click_speed_label = *find_descendant_of_type_named<GUI::Label>("double_click_speed_label");
    m_double_click_speed_slider = *find_descendant_of_type_named<GUI::HorizontalSlider>("double_click_speed_slider");
    m_double_click_speed_slider->set_min(WindowServer::double_click_speed_min);
    m_double_click_speed_slider->set_max(WindowServer::double_click_speed_max);
    m_double_click_speed_slider->set_value(GUI::ConnectionToWindowServer::the().get_double_click_speed(), GUI::AllowCallback::No);
    m_double_click_speed_slider->on_change = [&](int speed) {
        m_double_click_arrow_widget->set_double_click_speed(speed);
        update_double_click_speed_label();
        set_modified(true);
    };

    m_switch_buttons_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("switch_buttons_input");
    m_switch_buttons_checkbox->set_checked(GUI::ConnectionToWindowServer::the().get_buttons_switched(), GUI::AllowCallback::No);
    m_switch_buttons_checkbox->on_checked = [&](auto) {
        set_modified(true);
    };

    update_speed_label();
    update_double_click_speed_label();
    m_double_click_arrow_widget->set_double_click_speed(m_double_click_speed_slider->value());
}

void MouseWidget::apply_settings()
{
    float const factor = m_speed_slider->value() / speed_slider_scale;
    GUI::ConnectionToWindowServer::the().async_set_mouse_acceleration(factor);
    GUI::ConnectionToWindowServer::the().async_set_scroll_step_size(m_scroll_length_spinbox->value());
    GUI::ConnectionToWindowServer::the().async_set_double_click_speed(m_double_click_speed_slider->value());
    GUI::ConnectionToWindowServer::the().async_set_buttons_switched(m_switch_buttons_checkbox->is_checked());
}

void MouseWidget::reset_default_values()
{
    m_speed_slider->set_value(speed_slider_scale);
    m_scroll_length_spinbox->set_value(default_scroll_length);
    m_double_click_speed_slider->set_value(double_click_speed_default);
    m_switch_buttons_checkbox->set_checked(false);
}

void MouseWidget::update_speed_label()
{
    m_speed_label->set_text(String::formatted("{} %", m_speed_slider->value()));
}

void MouseWidget::update_double_click_speed_label()
{
    m_double_click_speed_label->set_text(String::formatted("{} ms", m_double_click_speed_slider->value()));
}
