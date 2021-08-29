/*
 * Copyright (c) 2021, Peter Elliott <pelliott@ualberta.ca>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DesktopStatusWindow.h"
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGUI/WindowManagerServerConnection.h>
#include <WindowServer/Window.h>
#include <serenity.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    ensure_pledge("stdio recvfd sendfd rpath unix", nullptr);

    auto app = GUI::Application::construct(argc, argv);

    // We need to obtain the WM connection here as well before the pledge shortening.
    GUI::WindowManagerServerConnection::the();

    ensure_pledge("stdio recvfd sendfd rpath", nullptr);

    auto window = DesktopStatusWindow::construct();
    window->set_title("DesktopPicker");
    window->resize(28, 16);
    window->show();
    window->make_window_manager(WindowServer::WMEventMask::VirtualDesktopChanges);

    return app->exec();
}
