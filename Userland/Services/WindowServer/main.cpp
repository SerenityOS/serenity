/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int, char**)
{
    if (pledge("stdio video thread sendfd recvfd accept rpath wpath cpath unix proc sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil /res");
        return 1;
    }

    if (unveil("/tmp", "cw") < 0) {
        perror("unveil /tmp cw");
        return 1;
    }

    if (unveil("/etc/WindowServer.ini", "rwc") < 0) {
        perror("unveil /etc/WindowServer.ini");
        return 1;
    }

    if (unveil("/dev", "rw") < 0) {
        perror("unveil /dev rw");
        return 1;
    }

    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    int rc = sigaction(SIGCHLD, &act, nullptr);
    if (rc < 0) {
        perror("sigaction");
        return 1;
    }

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

    if (pledge("stdio video thread sendfd recvfd accept rpath wpath cpath proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // First check which screens are explicitly configured
    {
        YAK::HashTable<String> fb_devices_configured;
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

    if (unveil("/tmp", "") < 0) {
        perror("unveil /tmp");
        return 1;
    }

    // NOTE: Because we dynamically need to be able to open new /dev/fb*
    // devices we can't really unveil all of /dev unless we have some
    // other mechanism that can hand us file descriptors for these.

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    dbgln("Entering WindowServer main loop");
    loop.exec();
    VERIFY_NOT_REACHED();
}
