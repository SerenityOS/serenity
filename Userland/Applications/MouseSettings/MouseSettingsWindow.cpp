/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@gmail.com>
 * Copyright (c) 2021, the SerenityOS developers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "MouseSettingsWindow.h"
#include <Applications/MouseSettings/MouseSettingsWindowGML.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/Event.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>

constexpr double speed_slider_scale = 100.0;
constexpr int default_scroll_length = 4;
constexpr int double_click_speed_default = 250;

void MouseSettingsWindow::update_window_server()
{
    const float factor = m_speed_slider->value() / speed_slider_scale;
    GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetMouseAcceleration>(factor);
    GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetScrollStepSize>(m_scroll_length_spinbox->value());
    GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetDoubleClickSpeed>(m_double_click_speed_slider->value());
}

void MouseSettingsWindow::reset_default_values()
{
    m_speed_slider->set_value(speed_slider_scale);
    m_scroll_length_spinbox->set_value(default_scroll_length);
    m_double_click_speed_slider->set_value(double_click_speed_default);
    update_window_server();
}

MouseSettingsWindow::MouseSettingsWindow()
{
    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.load_from_gml(mouse_settings_window_gml);

    m_speed_label = *main_widget.find_descendant_of_type_named<GUI::Label>("speed_label");
    m_speed_slider = *main_widget.find_descendant_of_type_named<GUI::HorizontalSlider>("speed_slider");
    m_speed_slider->set_range(WindowServer::mouse_accel_min * speed_slider_scale, WindowServer::mouse_accel_max * speed_slider_scale);
    m_speed_slider->on_change = [&](const int value) {
        m_speed_label->set_text(String::formatted("{} %", value));
    };
    const int slider_value = speed_slider_scale * GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::GetMouseAcceleration>()->factor();
    m_speed_slider->set_value(slider_value);

    m_scroll_length_spinbox = *main_widget.find_descendant_of_type_named<GUI::SpinBox>("scroll_length_spinbox");
    m_scroll_length_spinbox->set_min(WindowServer::scroll_step_size_min);
    m_scroll_length_spinbox->set_value(GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::GetScrollStepSize>()->step_size());

    m_double_click_speed_label = *main_widget.find_descendant_of_type_named<GUI::Label>("double_click_speed_label");
    m_double_click_speed_slider = *main_widget.find_descendant_of_type_named<GUI::HorizontalSlider>("double_click_speed_slider");
    m_double_click_speed_slider->set_min(WindowServer::double_click_speed_min);
    m_double_click_speed_slider->set_max(WindowServer::double_click_speed_max);
    m_double_click_speed_slider->on_change = [&](const int value) {
        m_double_click_speed_label->set_text(String::formatted("{} ms", value));
    };
    m_double_click_speed_slider->set_value(GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::GetDoubleClickSpeed>()->speed());

    m_ok_button = *main_widget.find_descendant_of_type_named<GUI::Button>("ok_button");
    m_ok_button->on_click = [this](auto) {
        update_window_server();
        GUI::Application::the()->quit();
    };

    m_apply_button = *main_widget.find_descendant_of_type_named<GUI::Button>("apply_button");
    m_apply_button->on_click = [this](auto) {
        update_window_server();
    };

    m_reset_button = *main_widget.find_descendant_of_type_named<GUI::Button>("reset_button");
    m_reset_button->on_click = [this](auto) {
        reset_default_values();
    };
}

MouseSettingsWindow::~MouseSettingsWindow()
{
}
