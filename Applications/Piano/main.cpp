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

#include "Music.h"
#include "PianoWidget.h"
#include <LibAudio/AClientConnection.h>
#include <LibCore/CFile.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <LibThread/Thread.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);
    auto audio_client = AClientConnection::construct();
    audio_client->handshake();

    auto window = GWindow::construct();
    window->set_title("Piano");
    window->set_rect(100, 100, 512, 512);

    auto piano_widget = PianoWidget::construct();
    window->set_main_widget(piano_widget);
    window->show();
    window->set_icon(load_png("/res/icons/16x16/app-piano.png"));

    LibThread::Thread sound_thread([piano_widget = piano_widget.ptr()] {
        auto audio = CFile::construct("/dev/audio");
        if (!audio->open(CIODevice::WriteOnly)) {
            dbgprintf("Can't open audio device: %s", audio->error_string());
            return 1;
        }

        for (;;) {
            u8 buffer[4096];
            piano_widget->fill_audio_buffer(buffer, sizeof(buffer));
            audio->write(buffer, sizeof(buffer));
            CEventLoop::current().post_event(*piano_widget, make<CCustomEvent>(0));
            CEventLoop::wake();
        }
    });
    sound_thread.start();

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("Piano");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Piano", load_png("/res/icons/32x32/app-piano.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
