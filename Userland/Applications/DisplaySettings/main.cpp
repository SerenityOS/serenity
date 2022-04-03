/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BackgroundSettingsWidget.h"
#include "DesktopSettingsWidget.h"
#include "FontSettingsWidget.h"
#include "MonitorSettingsWidget.h"
#include "ThemesSettingsWidget.h"
#include <AK/Array.h>
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio thread recvfd sendfd rpath cpath wpath unix"));

    auto app = TRY(GUI::Application::try_create(arguments));
    Config::pledge_domain("WindowManager");

    String tab_to_open;
    Core::ArgsParser args_parser;
    args_parser.add_option(tab_to_open, "Settings tab to open", "settings-tab", 't', "settings-tab");
    args_parser.parse(arguments);

    auto app_icon = GUI::Icon::default_icon("app-display-settings");

    struct SettingsTab {
        StringView name;
        GUI::Widget* widget;
    };

    auto window = TRY(GUI::SettingsWindow::create("Display Settings"));
    window->set_icon(app_icon.bitmap_for_size(16));

    auto add_tab = [&]<class T, class... Args>(StringView name, Args&&... args) {
        return SettingsTab { name, MUST(window->add_tab<T>(name, forward<Args>(args)...)) };
    };

    bool background_settings_changed = false;
    Array settings_tabs {
        add_tab.template operator()<DisplaySettings::BackgroundSettingsWidget>("Background", background_settings_changed),
        add_tab.template operator()<DisplaySettings::ThemesSettingsWidget>("Themes", background_settings_changed),
        add_tab.template operator()<DisplaySettings::FontSettingsWidget>("Fonts"),
        add_tab.template operator()<DisplaySettings::MonitorSettingsWidget>("Monitor"),
        add_tab.template operator()<DisplaySettings::DesktopSettingsWidget>("Workspaces")
    };

    if (!tab_to_open.is_empty()) {
        for (auto& tab : settings_tabs) {
            if (tab_to_open.equals_ignoring_case(tab.name))
                window->set_active_widget(tab.widget);
        }
    }

    window->show();
    return app->exec();
}
