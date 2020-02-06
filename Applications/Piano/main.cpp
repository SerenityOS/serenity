/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
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

#include "AudioEngine.h"
#include "MainWidget.h"
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
    GUI::Application app(argc, argv);

    auto audio_client = Audio::ClientConnection::construct();
    audio_client->handshake();

    AudioEngine audio_engine;

    auto window = GUI::Window::construct();
    auto main_widget = MainWidget::construct(audio_engine);
    window->set_main_widget(main_widget);
    window->set_title("Piano");
    window->set_rect(90, 90, 840, 600);
    window->set_icon(Gfx::load_png("/res/icons/16x16/app-piano.png"));
    window->show();

    LibThread::Thread audio_thread([&] {
        auto audio = Core::File::construct("/dev/audio");
        if (!audio->open(Core::IODevice::WriteOnly)) {
            dbgprintf("Can't open audio device: %s", audio->error_string());
            return 1;
        }

        FixedArray<Sample> buffer(sample_count);
        for (;;) {
            audio_engine.fill_buffer(buffer);
            audio->write(reinterpret_cast<u8*>(buffer.data()), buffer_size);
            Core::EventLoop::current().post_event(*main_widget, make<Core::CustomEvent>(0));
            Core::EventLoop::wake();
        }
    });
    audio_thread.start();

    auto menubar = make<GUI::MenuBar>();

    auto app_menu = GUI::Menu::construct("Piano");
    app_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Piano", Gfx::load_png("/res/icons/32x32/app-piano.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
