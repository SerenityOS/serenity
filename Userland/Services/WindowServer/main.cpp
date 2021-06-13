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
    AK::HashTable<String> fb_devices_configured;
    int main_screen_index = wm_config->read_num_entry("Screens", "MainScreen", 0);
    for (int screen_index = 0;; screen_index++) {
        auto group_name = String::formatted("Screen{}", screen_index);
        if (!wm_config->has_group(group_name))
            break;

        int scale = wm_config->read_num_entry(group_name, "ScaleFactor", 1);
        auto device_path = wm_config->read_entry(group_name, "Device", {});
        if (device_path.is_null() || device_path.is_empty()) {
            dbgln("Screen {} misses Device setting", screen_index);
            break;
        }

        Gfx::IntRect virtual_rect {
            wm_config->read_num_entry(group_name, "Left", 0 / scale),
            wm_config->read_num_entry(group_name, "Top", 0 / scale),
            wm_config->read_num_entry(group_name, "Width", 1024 / scale),
            wm_config->read_num_entry("Screen", "Height", 768 / scale)
        };
        auto* screen = WindowServer::Screen::create(device_path, virtual_rect, scale);
        if (!screen) {
            dbgln("Screen {} failed to be created", screen_index);
            break;
        }

        if (main_screen_index == screen_index)
            screen->make_main_screen();

        // Remember that we used this device for a screen already
        fb_devices_configured.set(device_path);
    }

    // TODO: Enumerate the /dev/fbX devices and set up any ones we find that we haven't already used

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

    if (unveil("/dev", "") < 0) {
        perror("unveil /dev");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    dbgln("Entering WindowServer main loop");
    loop.exec();
    VERIFY_NOT_REACHED();
}
