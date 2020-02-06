/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Compositor.h"
#include "EventLoop.h"
#include "Screen.h"
#include "WindowManager.h"
#include <LibCore/ConfigFile.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <signal.h>
#include <stdio.h>

int main(int, char**)
{
    if (pledge("stdio video thread shared_buffer accept rpath wpath cpath unix proc exec fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp", "cw") < 0) {
        perror("unveil");
        return 1;
    }

    // FIXME: WindowServer should obviously not hardcode this.
    //        Instead, we should have a ConfigServer or similar that allows programs
    //        to get/set user settings over IPC without giving them access to any files.
    if (unveil("/home/anon/WindowManager.ini", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/dev", "rw") < 0) {
        perror("unveil");
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

    auto wm_config = Core::ConfigFile::get_for_app("WindowManager");
    auto theme_name = wm_config->read_entry("Theme", "Name", "Default");

    auto theme = Gfx::load_system_theme(String::format("/res/themes/%s.ini", theme_name.characters()));
    ASSERT(theme);
    Gfx::set_system_theme(*theme);
    auto palette = Gfx::PaletteImpl::create_with_shared_buffer(*theme);

    WindowServer::EventLoop loop;

    if (pledge("stdio video thread shared_buffer accept rpath wpath cpath proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    WindowServer::Screen screen(wm_config->read_num_entry("Screen", "Width", 1024),
        wm_config->read_num_entry("Screen", "Height", 768));
    WindowServer::Compositor::the();
    auto wm = WindowServer::WindowManager::construct(*palette);
    auto mm = WindowServer::MenuManager::construct();

    if (unveil("/tmp", "") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/dev", "") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    dbgprintf("Entering WindowServer main loop.\n");
    loop.exec();
    ASSERT_NOT_REACHED();
}
