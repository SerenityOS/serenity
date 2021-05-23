/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "TrackManager.h"
#include <AK/Array.h>
#include <LibAudio/ClientConnection.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibThreading/Thread.h>

int main(int argc, char** argv)
{
    if (pledge("stdio thread rpath cpath wpath recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    auto audio_client = Audio::ClientConnection::construct();

    TrackManager track_manager;

    auto app_icon = GUI::Icon::default_icon("app-piano");
    auto window = GUI::Window::construct();
    auto& main_widget = window->set_main_widget<MainWidget>(track_manager);
    window->set_title("Piano");
    window->resize(840, 600);
    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();

    Audio::WavWriter wav_writer;
    Optional<String> save_path;
    bool need_to_write_wav = false;

    auto audio_thread = Threading::Thread::construct([&] {
        auto audio = Core::File::construct("/dev/audio");
        if (!audio->open(Core::OpenMode::WriteOnly)) {
            dbgln("Can't open audio device: {}", audio->error_string());
            return 1;
        }

        Array<Sample, sample_count> buffer;
        while (!Core::EventLoop::current().was_exit_requested()) {
            track_manager.fill_buffer(buffer);
            audio->write(reinterpret_cast<u8*>(buffer.data()), buffer_size);
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
        return 0;
    });
    audio_thread->start();

    auto main_widget_updater = Core::Timer::construct(150, [&] {
        Core::EventLoop::current().post_event(main_widget, make<Core::CustomEvent>(0));
    });
    main_widget_updater->start();

    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(GUI::Action::create("Export", { Mod_Ctrl, Key_E }, [&](const GUI::Action&) {
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
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
        return;
    }));

    auto& edit_menu = menubar->add_menu("&Edit");
    main_widget.add_actions(edit_menu);

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Piano", app_icon, window));

    window->set_menubar(move(menubar));

    return app->exec();
}
