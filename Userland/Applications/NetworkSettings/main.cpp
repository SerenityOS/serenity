/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NetworkSettingsWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath recvfd sendfd unix proc exec"));

    TRY(Core::System::unveil("/bin/Escalator", "x"));
    TRY(Core::System::unveil("/etc/Network.ini", "r"));
    TRY(Core::System::unveil("/sys/kernel/net/adapters", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/clipboard", "rw"));
    TRY(Core::System::unveil("/tmp/portal/window", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView adapter;

    Core::ArgsParser parser;
    parser.add_positional_argument(adapter, "Adapter to display settings for", "adapter", Core::ArgsParser::Required::No);
    parser.parse(args);

    auto app = TRY(GUI::Application::create(args));

    TRY(Core::System::pledge("stdio rpath wpath cpath recvfd sendfd proc exec"));

    auto app_icon = GUI::Icon::default_icon("network"sv);
    auto window = TRY(GUI::SettingsWindow::create("Network Settings", GUI::SettingsWindow::ShowDefaultsButton::No));

    auto network_settings_widget = TRY(NetworkSettings::NetworkSettingsWidget::try_create());
    TRY(window->add_tab(network_settings_widget, "Network"_string, "network"sv));
    if (!adapter.is_null()) {
        network_settings_widget->switch_adapter(adapter);
    }
    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
