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

#include "MainWidget.h"
#include "TrackManager.h"
#include <AK/Array.h>
#include <LibAudio/ClientConnection.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibThread/Thread.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto audio_client = Audio::ClientConnection::construct();
    audio_client->handshake();

    TrackManager track_manager;

    auto window = GUI::Window::construct();
    auto& main_widget = window->set_main_widget<MainWidget>(track_manager);
    window->set_title("Piano");
    window->resize(840, 600);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-piano.png"));
    window->show();

    Audio::WavWriter wav_writer;
    Optional<String> save_path;
    bool need_to_write_wav = false;

    LibThread::Thread audio_thread([&] {
        auto audio = Core::File::construct("/dev/audio");
        if (!audio->open(Core::IODevice::WriteOnly)) {
            dbgln("Can't open audio device: {}", audio->error_string());
            return 1;
        }

        Array<Sample, sample_count> buffer;
        for (;;) {
            track_manager.fill_buffer(buffer);
            audio->write(reinterpret_cast<u8*>(buffer.data()), buffer_size);
            Core::EventLoop::current().post_event(main_widget, make<Core::CustomEvent>(0));
            Core::EventLoop::wake();

            if (need_to_write_wav) {
                need_to_write_wav = false;
                track_manager.reset();
                track_manager.set_should_loop(false);
                do {
                    track_manager.fill_buffer(buffer);
                    wav_writer.write_samples(reinterpret_cast<u8*>(buffer.data()), buffer_size);
                } while (track_manager.time());
                track_manager.reset();
                track_manager.set_should_loop(true);
                wav_writer.finalize();
            }
        }
    });
    audio_thread.start();

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("Piano");
    app_menu.add_action(GUI::Action::create("Export", { Mod_Ctrl, Key_E }, [&](const GUI::Action&) {
        save_path = GUI::FilePicker::get_save_filepath(window, "Untitled", "wav");
        if (!save_path.has_value())
            return;
        wav_writer.set_file(save_path.value());
        if (wav_writer.has_error()) {
            GUI::MessageBox::show(window, String::formatted("Failed to export WAV file: {}", wav_writer.error_string()), "Error", GUI::MessageBox::Type::Error);
            wav_writer.clear_error();
            return;
        }
        need_to_write_wav = true;
    }));
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
        return;
    }));

    auto& edit_menu = menubar->add_menu("Edit");
    main_widget.add_actions(edit_menu);

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Piano", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-piano.png"), window);
    }));

    app->set_menubar(move(menubar));

    return app->exec();
}
