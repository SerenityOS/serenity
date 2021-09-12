/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DesktopStatusWindow.h"
#include <LibGUI/Application.h>
#include <LibGUI/WindowManagerServerConnection.h>
#include <WindowServer/Window.h>
#include <serenity.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    // We need to obtain the WM connection here as well before the pledge shortening.
    GUI::WindowManagerServerConnection::the();

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = DesktopStatusWindow::construct();
    window->set_title("DesktopPicker");
    window->resize(28, 16);
    window->show();
    window->make_window_manager(WindowServer::WMEventMask::VirtualDesktopChanges);

    return app->exec();
}
