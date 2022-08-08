/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/WindowManager.h>
#include <WindowServer/Window.h>
#include <WindowServer/WindowType.h>

class WinlistWindowManager : public GUI::WindowManager {
public:
    void event(Core::Event& event) override;
};

bool keep_alive = false;
StringView format;
void WinlistWindowManager::event(Core::Event& event)
{
    if (event.type() == GUI::Event::WM_WindowStateChanged) {
        auto& changed_event = static_cast<GUI::WMWindowStateChangedEvent&>(event);
        StringBuilder builder;

        if (format.is_empty())
            format = "%i %t"sv;
        for (size_t i = 0; i < format.length(); i++) {
            char c = format[i];
            if (c != '%') {
                builder.append(c);
            } else {
                if (++i >= format.length())
                    break;
                switch (format[i]) {
                case 'i':
                    builder.appendff("{}", changed_event.window_id());
                    break;
                case 't':
                    builder.append(changed_event.title());
                    break;
                case '%':
                    builder.append('%');
                    break;
                }
            }
        }

        outln(builder.build());
    } else if (event.type() == GUI::Event::WM_GreetingIsOver && !keep_alive) {
        exit(0);
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio unix recvfd sendfd"));

    bool titles = false;
    bool ids = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(format, "Output format (defaults to %i %t)", "format", 'f', "format");
    args_parser.add_option(titles, "Equivalent to -f %t", "titles", 't');
    args_parser.add_option(ids, "Equivalent to -f %i", "ids", 'i');
    args_parser.add_option(keep_alive, "Don't close connection to WindowServer; listen for new windows", "keep-alive", 'k');
    args_parser.parse(arguments);

    if (format.is_empty()) {
        if (titles && ids)
            format = "%i %t"sv;
        else if (titles)
            format = "%t"sv;
        else if (ids)
            format = "%i"sv;
    }

    Core::EventLoop event_loop;

    WinlistWindowManager wm;
    GUI::ConnectionToWindowManagerServer::the().async_set_event_mask(
        WindowServer::WMEventMask::WindowStateChanges);
    GUI::ConnectionToWindowManagerServer::the().async_set_window_manager(wm.wm_id(), false);

    TRY(Core::System::pledge("stdio recvfd"));

    event_loop.exec();

    return 0;
}
