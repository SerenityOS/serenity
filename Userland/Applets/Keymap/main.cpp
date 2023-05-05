/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeymapStatusWindow.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibMain/Main.h>
#include <WindowServer/Window.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix getkeymap proc exec"));

    auto app = TRY(GUI::Application::create(arguments));

    auto window = TRY(KeymapStatusWindow::try_create());
    window->set_has_alpha_channel(true);
    window->set_title("Keymap");
    window->resize(16, 16);
    window->show();
    window->make_window_manager(WindowServer::WMEventMask::KeymapChanged);

    TRY(Core::System::pledge("stdio recvfd sendfd rpath getkeymap proc exec"));

    return app->exec();
}
