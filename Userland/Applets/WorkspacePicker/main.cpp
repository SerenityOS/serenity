/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DesktopStatusWindow.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Painter.h>
#include <LibMain/Main.h>
#include <WindowServer/Window.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath proc exec unix"));

    auto app = TRY(GUI::Application::create(arguments));
    app->set_quit_when_last_window_deleted(false);

    // We need to obtain the WM connection here as well before the pledge shortening.
    GUI::ConnectionToWindowManagerServer::the();

    TRY(Core::System::pledge("stdio recvfd sendfd rpath proc exec"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin/DisplaySettings", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(DesktopStatusWindow::try_create());
    window->set_title("WorkspacePicker");
    window->resize(28, 15);

    auto& desktop = GUI::Desktop::the();

    auto hide_tray_icon = [&] {
        window->hide();
    };

    auto show_tray_icon = [&] {
        if (!window->is_visible()) {
            window->show();
            window->make_window_manager(WindowServer::WMEventMask::WorkspaceChanges);
        }
    };

    if (desktop.workspace_rows() != 1 || desktop.workspace_columns() != 1) {
        show_tray_icon();
    }

    desktop.on_receive_screen_rects([&](auto&) {
        if (desktop.workspace_rows() == 1 && desktop.workspace_columns() == 1) {
            hide_tray_icon();
        } else {
            window->update();
            show_tray_icon();
        }
    });

    return app->exec();
}
