/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeymapStatusWindow.h"
#include "KeymapWindowManager.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibMain/Main.h>
#include <WindowServer/Window.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix getkeymap proc exec"));

    auto app = TRY(GUI::Application::try_create(arguments));

    auto window = TRY(KeymapStatusWindow::try_create());
    KeymapWindowManager wm(window);
    window->set_has_alpha_channel(true);
    window->set_title("Keymap");
    window->resize(16, 16);
    window->show();

    GUI::ConnectionToWindowManagerServer::the().async_set_event_mask(
        WindowServer::WMEventMask::KeymapChanged);
    GUI::ConnectionToWindowManagerServer::the().async_set_window_manager(wm.wm_id(), false);

    TRY(Core::System::pledge("stdio recvfd sendfd rpath getkeymap proc exec"));

    return app->exec();
}
