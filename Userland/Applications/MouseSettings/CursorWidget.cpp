/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CursorWidget.h"

#include <Applications/MouseSettings/CursorWidgetGML.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>

constexpr double speed_slider_scale = 100.0;
constexpr int default_cursor_size = 1;

CursorWidget::CursorWidget()
{
    load_from_gml(cursor_widget_gml);

    m_speed_label = *find_descendant_of_type_named<GUI::Label>("speed_label");
    m_speed_slider = *find_descendant_of_type_named<GUI::HorizontalSlider>("speed_slider");
    m_speed_slider->set_range(WindowServer::mouse_accel_min * speed_slider_scale, WindowServer::mouse_accel_max * speed_slider_scale);
    m_speed_slider->on_change = [&](int value) {
        m_speed_label->set_text(String::formatted("{} %", value));
    };
    int const speed_slider_value = float { speed_slider_scale } * GUI::ConnectionToWindowServer::the().get_mouse_acceleration();
    m_speed_slider->set_value(speed_slider_value);

    m_size_label = *find_descendant_of_type_named<GUI::Label>("size_label");
    m_size_slider = *find_descendant_of_type_named<GUI::HorizontalSlider>("size_slider");
    m_size_slider->set_range(WindowServer::mouse_size_min, WindowServer::mouse_size_max);
    m_size_slider->on_change = [&](int value) {
        m_size_label->set_text(String::formatted("{}x", value));
    };
    int const size_slider_value = GUI::ConnectionToWindowServer::the().get_cursor_size();
    m_size_slider->set_value(size_slider_value);
};

void CursorWidget::apply_settings()
{
    float const factor = m_speed_slider->value() / speed_slider_scale;
    GUI::ConnectionToWindowServer::the().async_set_mouse_acceleration(factor);
    GUI::ConnectionToWindowServer::the().async_set_cursor_size(m_size_slider->value());
}

void CursorWidget::reset_default_values()
{
    m_speed_slider->set_value(speed_slider_scale);
    m_size_slider->set_value(default_cursor_size);
}
