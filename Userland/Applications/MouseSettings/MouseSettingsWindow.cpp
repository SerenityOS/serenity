/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MouseSettingsWindow.h"
#include "DoubleClickArrowWidget.h"
#include <Applications/MouseSettings/MouseSettingsWindowGML.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Widget.h>
#include <LibGUI/WindowServerConnection.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>

constexpr double speed_slider_scale = 100.0;
constexpr int default_scroll_length = 4;
constexpr int double_click_speed_default = 250;

void MouseSettingsWindow::update_window_server()
{
    const float factor = m_speed_slider->value() / speed_slider_scale;
    GUI::WindowServerConnection::the().async_set_mouse_acceleration(factor);
    GUI::WindowServerConnection::the().async_set_scroll_step_size(m_scroll_length_spinbox->value());
    GUI::WindowServerConnection::the().async_set_double_click_speed(m_double_click_speed_slider->value());
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
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_margins(4);
    main_widget.layout()->set_spacing(6);

    auto& tab_widget = main_widget.add<GUI::TabWidget>();
    auto& mouse_widget = tab_widget.add_tab<GUI::Widget>("Mouse");
    mouse_widget.load_from_gml(mouse_settings_window_gml);

    m_speed_label = *main_widget.find_descendant_of_type_named<GUI::Label>("speed_label");
    m_speed_slider = *main_widget.find_descendant_of_type_named<GUI::HorizontalSlider>("speed_slider");
    m_speed_slider->set_range(WindowServer::mouse_accel_min * speed_slider_scale, WindowServer::mouse_accel_max * speed_slider_scale);
    m_speed_slider->on_change = [&](int value) {
        m_speed_label->set_text(String::formatted("{} %", value));
    };
    const int slider_value = float { speed_slider_scale } * GUI::WindowServerConnection::the().get_mouse_acceleration();
    m_speed_slider->set_value(slider_value);

    auto& cursor_speed_image_label = *main_widget.find_descendant_of_type_named<GUI::Label>("cursor_speed_image_label");
    cursor_speed_image_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/graphics/mouse-cursor-speed.png"));

    auto& scroll_step_size_image_label = *main_widget.find_descendant_of_type_named<GUI::Label>("scroll_step_size_image_label");
    scroll_step_size_image_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/graphics/scroll-wheel-step-size.png"));

    m_scroll_length_spinbox = *main_widget.find_descendant_of_type_named<GUI::SpinBox>("scroll_length_spinbox");
    m_scroll_length_spinbox->set_min(WindowServer::scroll_step_size_min);
    m_scroll_length_spinbox->set_value(GUI::WindowServerConnection::the().get_scroll_step_size());

    m_double_click_arrow_widget = *main_widget.find_descendant_of_type_named<MouseSettings::DoubleClickArrowWidget>("double_click_arrow_widget");
    m_double_click_speed_label = *main_widget.find_descendant_of_type_named<GUI::Label>("double_click_speed_label");
    m_double_click_speed_slider = *main_widget.find_descendant_of_type_named<GUI::HorizontalSlider>("double_click_speed_slider");
    m_double_click_speed_slider->set_min(WindowServer::double_click_speed_min);
    m_double_click_speed_slider->set_max(WindowServer::double_click_speed_max);
    m_double_click_speed_slider->on_change = [&](int speed) {
        m_double_click_arrow_widget->set_double_click_speed(speed);
        m_double_click_speed_label->set_text(String::formatted("{} ms", speed));
    };
    m_double_click_speed_slider->set_value(GUI::WindowServerConnection::the().get_double_click_speed());

    auto& button_container = main_widget.add<GUI::Widget>();
    button_container.set_shrink_to_fit(true);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.layout()->set_spacing(6);

    m_reset_button = button_container.add<GUI::Button>("Defaults");
    m_reset_button->on_click = [this](auto) {
        reset_default_values();
    };

    button_container.layout()->add_spacer();

    m_ok_button = button_container.add<GUI::Button>("OK");
    m_ok_button->set_fixed_width(75);
    m_ok_button->on_click = [&](auto) {
        update_window_server();
        GUI::Application::the()->quit();
    };

    m_cancel_button = button_container.add<GUI::Button>("Cancel");
    m_cancel_button->set_fixed_width(75);
    m_cancel_button->on_click = [&](auto) {
        GUI::Application::the()->quit();
    };

    m_apply_button = button_container.add<GUI::Button>("Apply");
    m_apply_button->set_fixed_width(75);
    m_apply_button->on_click = [&](auto) {
        update_window_server();
    };
}

MouseSettingsWindow::~MouseSettingsWindow()
{
}
