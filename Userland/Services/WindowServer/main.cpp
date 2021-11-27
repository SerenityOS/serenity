/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AppletManager.h"
#include "Compositor.h"
#include "EventLoop.h"
#include "Screen.h"
#include "WindowManager.h"
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <LibMain/Main.h>
#include <signal.h>
#include <string.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio video thread sendfd recvfd accept rpath wpath cpath unix proc sigaction"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp", "cw"));
    TRY(Core::System::unveil("/etc/WindowServer.ini", "rwc"));
    TRY(Core::System::unveil("/dev", "rw"));

    struct sigaction act = {};
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    TRY(Core::System::sigaction(SIGCHLD, &act, nullptr));

    auto wm_config = Core::ConfigFile::open("/etc/WindowServer.ini");
    auto theme_name = wm_config->read_entry("Theme", "Name", "Default");

    auto theme = Gfx::load_system_theme(String::formatted("/res/themes/{}.ini", theme_name));
    VERIFY(theme.is_valid());
    Gfx::set_system_theme(theme);
    auto palette = Gfx::PaletteImpl::create_with_anonymous_buffer(theme);

    auto default_font_query = wm_config->read_entry("Fonts", "Default", "Katica 10 400");
    auto fixed_width_font_query = wm_config->read_entry("Fonts", "FixedWidth", "Csilla 10 400");

    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);

    WindowServer::EventLoop loop;

    TRY(Core::System::pledge("stdio video thread sendfd recvfd accept rpath wpath cpath proc"));

    // First check which screens are explicitly configured
    {
        AK::HashTable<String> fb_devices_configured;
        WindowServer::ScreenLayout screen_layout;
        String error_msg;

        auto add_unconfigured_devices = [&]() {
            // Enumerate the /dev/fbX devices and try to set up any ones we find that we haven't already used
            Core::DirIterator di("/dev", Core::DirIterator::SkipParentAndBaseDir);
            while (di.has_next()) {
                auto path = di.next_path();
                if (!path.starts_with("fb"))
                    continue;
                auto full_path = String::formatted("/dev/{}", path);
                if (!Core::File::is_device(full_path))
                    continue;
                if (fb_devices_configured.find(full_path) != fb_devices_configured.end())
                    continue;
                if (!screen_layout.try_auto_add_framebuffer(full_path))
                    dbgln("Could not auto-add framebuffer device {} to screen layout", full_path);
            }
        };

        auto apply_and_generate_generic_screen_layout = [&]() {
            screen_layout = {};
            fb_devices_configured = {};
            add_unconfigured_devices();
            if (!WindowServer::Screen::apply_layout(move(screen_layout), error_msg)) {
                dbgln("Failed to apply generated fallback screen layout: {}", error_msg);
                return false;
            }

            dbgln("Applied generated fallback screen layout!");
            return true;
        };

        if (screen_layout.load_config(*wm_config, &error_msg)) {
            for (auto& screen_info : screen_layout.screens)
                fb_devices_configured.set(screen_info.device);

            add_unconfigured_devices();

            if (!WindowServer::Screen::apply_layout(move(screen_layout), error_msg)) {
                dbgln("Error applying screen layout: {}", error_msg);
                if (!apply_and_generate_generic_screen_layout())
                    return 1;
            }
        } else {
            dbgln("Error loading screen configuration: {}", error_msg);
            if (!apply_and_generate_generic_screen_layout())
                return 1;
        }
    }

    auto& screen_input = WindowServer::ScreenInput::the();
    screen_input.set_cursor_location(WindowServer::Screen::main().rect().center());
    screen_input.set_acceleration_factor(atof(wm_config->read_entry("Mouse", "AccelerationFactor", "1.0").characters()));
    screen_input.set_scroll_step_size(wm_config->read_num_entry("Mouse", "ScrollStepSize", 4));

    WindowServer::Compositor::the();
    auto wm = WindowServer::WindowManager::construct(*palette);
    auto am = WindowServer::AppletManager::construct();
    auto mm = WindowServer::MenuManager::construct();

    TRY(Core::System::unveil("/tmp", ""));

    // NOTE: Because we dynamically need to be able to open new /dev/fb*
    // devices we can't really unveil all of /dev unless we have some
    // other mechanism that can hand us file descriptors for these.

    TRY(Core::System::unveil(nullptr, nullptr));

    dbgln("Entering WindowServer main loop");
    loop.exec();
    VERIFY_NOT_REACHED();
}
