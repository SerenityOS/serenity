/*
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CertificateStoreWidget.h"
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(args));

    TRY(Core::System::unveil(TRY(String::formatted("{}/.config/certs.pem", Core::StandardPaths::home_directory())), "rwc"_string));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/etc/cacert.pem", "r"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("certificate"sv);
    auto window = TRY(GUI::SettingsWindow::create("Certificate Settings", GUI::SettingsWindow::ShowDefaultsButton::No));
    (void)TRY(window->add_tab<CertificateSettings::CertificateStoreWidget>("Certificate Store"_string, "certificate"sv));
    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
