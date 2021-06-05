/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BackgroundSettingsWidget.h"
#include "FontSettingsWidget.h"
#include "MonitorSettingsWidget.h"
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio thread recvfd sendfd rpath cpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio thread recvfd sendfd rpath cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-display-settings");

    auto window = GUI::Window::construct();
    window->set_title("Display Settings");
    window->resize(400, 480);
    window->set_resizable(false);

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_margins({ 4, 4, 4, 4 });
    main_widget.layout()->set_spacing(6);

    auto& tab_widget = main_widget.add<GUI::TabWidget>();
    auto& background_settings_widget = tab_widget.add_tab<DisplaySettings::BackgroundSettingsWidget>("Background");
    auto& font_settings_widget = tab_widget.add_tab<DisplaySettings::FontSettingsWidget>("Fonts");
    auto& monitor_settings_widget = tab_widget.add_tab<DisplaySettings::MonitorSettingsWidget>("Monitor");

    auto& button_container = main_widget.add<GUI::Widget>();
    button_container.set_shrink_to_fit(true);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.layout()->set_spacing(6);
    button_container.layout()->add_spacer();

    auto& ok_button = button_container.add<GUI::Button>("OK");
    ok_button.set_fixed_width(75);
    ok_button.on_click = [&](auto) {
        background_settings_widget.apply_settings();
        monitor_settings_widget.apply_settings();
        font_settings_widget.apply_settings();
        app->quit();
    };

    auto& cancel_button = button_container.add<GUI::Button>("Cancel");
    cancel_button.set_fixed_width(75);
    cancel_button.on_click = [&](auto) {
        app->quit();
    };

    auto& apply_button = button_container.add<GUI::Button>("Apply");
    apply_button.set_fixed_width(75);
    apply_button.on_click = [&](auto) {
        background_settings_widget.apply_settings();
        monitor_settings_widget.apply_settings();
        font_settings_widget.apply_settings();
    };

    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
