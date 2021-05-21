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

    int scale = wm_config->read_num_entry("Screen", "ScaleFactor", 1);
    WindowServer::Screen screen(wm_config->read_num_entry("Screen", "Width", 1024 / scale), wm_config->read_num_entry("Screen", "Height", 768 / scale), scale);
    screen.set_acceleration_factor(atof(wm_config->read_entry("Mouse", "AccelerationFactor", "1.0").characters()));
    screen.set_scroll_step_size(wm_config->read_num_entry("Mouse", "ScrollStepSize", 4));
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
