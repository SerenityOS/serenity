/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@gmail.com>
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

#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/SystemTheme.h>
#include <WindowServer/Screen.h>

int main(int argc, char** argv)
{
    if (pledge("stdio cpath rpath recvfd sendfd unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio cpath rpath recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-mouse");
    auto window = GUI::Window::construct();
    window->set_title("Mouse Settings");
    window->resize(200, 130);
    window->set_resizable(false);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& settings = window->set_main_widget<GUI::Widget>();
    settings.set_fill_with_background_color(true);
    settings.set_background_role(ColorRole::Button);
    settings.set_layout<GUI::VerticalBoxLayout>();
    settings.layout()->set_margins({ 4, 4, 4, 4 });

    auto& speed_container = settings.add<GUI::GroupBox>("Mouse speed");
    speed_container.set_layout<GUI::VerticalBoxLayout>();
    speed_container.layout()->set_margins({ 6, 16, 6, 6 });
    speed_container.set_fixed_height(50);

    auto& speed_slider = speed_container.add<GUI::HorizontalSlider>();
    const auto scalar = 1000.0;
    speed_slider.set_range(WindowServer::mouse_accel_min * scalar, WindowServer::mouse_accel_max * scalar); // These values are scaled down (by a factor of 1000) to get fractional values
    int current_value = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::GetMouseAcceleration>()->factor() * scalar;
    speed_slider.set_value(current_value);

    auto& scroll_container = settings.add<GUI::GroupBox>("Scroll length");
    scroll_container.set_layout<GUI::VerticalBoxLayout>();
    scroll_container.layout()->set_margins({ 6, 16, 6, 6 });
    scroll_container.set_fixed_height(46);

    auto& scroll_spinbox = scroll_container.add<GUI::SpinBox>();
    scroll_spinbox.set_min(WindowServer::scroll_step_size_min);
    scroll_spinbox.set_value(GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::GetScrollStepSize>()->step_size());

    auto update_window_server = [&]() {
        float factor = speed_slider.value() / scalar;
        GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetMouseAcceleration>(factor);
        GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetScrollStepSize>(scroll_spinbox.value());
    };

    auto& prompt_buttons = settings.add<GUI::Widget>();
    prompt_buttons.set_layout<GUI::HorizontalBoxLayout>();
    prompt_buttons.set_fixed_height(22);

    auto& ok_button = prompt_buttons.add<GUI::Button>();
    ok_button.set_text("OK");
    prompt_buttons.set_fixed_height(22);
    ok_button.on_click = [&](auto) {
        update_window_server();
        app->quit();
    };
    auto& apply_button = prompt_buttons.add<GUI::Button>();
    apply_button.set_text("Apply");
    prompt_buttons.set_fixed_height(22);
    apply_button.on_click = [&](auto) {
        update_window_server();
    };
    auto& reset_button = prompt_buttons.add<GUI::Button>();
    reset_button.set_text("Reset");
    prompt_buttons.set_fixed_height(22);
    reset_button.on_click = [&](auto) {
        speed_slider.set_value(scalar);
        scroll_spinbox.set_value(4);
        update_window_server();
    };

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("File");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));
    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Mouse Settings", app_icon, window));
    window->set_menubar(move(menubar));

    window->show();
    return app->exec();
}
