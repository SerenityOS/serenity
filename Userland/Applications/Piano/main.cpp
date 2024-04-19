/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AudioPlayerLoop.h"
#include "ExportProgressWindow.h"
#include "MainWidget.h"
#include "TrackManager.h"
#include <AK/Atomic.h>
#include <AK/Queue.h>
#include <LibAudio/ConnectionToServer.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibThreading/MutexProtected.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio thread proc rpath cpath wpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(arguments));

    TrackManager track_manager;

    Threading::MutexProtected<Audio::WavWriter> wav_writer;
    Optional<ByteString> save_path;
    Atomic<bool> need_to_write_wav = false;
    Atomic<int> wav_percent_written = 0;

    auto audio_loop = AudioPlayerLoop::construct(track_manager, need_to_write_wav, wav_percent_written, wav_writer);

    auto app_icon = GUI::Icon::default_icon("app-piano"sv);
    auto window = GUI::Window::construct();
    auto main_widget = TRY(MainWidget::try_create(track_manager, audio_loop));
    window->set_main_widget(main_widget);
    window->set_title("Piano");
    window->restore_size_and_position("Piano"sv, "Window"sv, { { 840, 600 } });
    window->save_size_and_position_on_close("Piano"sv, "Window"sv);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto wav_progress_window = ExportProgressWindow::construct(*window, wav_percent_written);
    TRY(wav_progress_window->initialize());

    auto main_widget_updater = Core::Timer::create_repeating(static_cast<int>((1 / 30.0) * 1000), [&] {
        if (window->is_active())
            Core::EventLoop::current().post_event(main_widget, make<Core::CustomEvent>(0));
    });
    main_widget_updater->start();

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::Action::create("Export...", { Mod_Ctrl, Key_E }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/file-export.png"sv)), [&](const GUI::Action&) {
        save_path = GUI::FilePicker::get_save_filepath(window, "Untitled", "wav");
        if (!save_path.has_value())
            return;
        ByteString error;
        wav_writer.with_locked([&](auto& wav_writer) {
            auto error_or_void = wav_writer.set_file(save_path.value());
            if (error_or_void.is_error())
                error = ByteString::formatted("Failed to export WAV file: {}", error_or_void.error());
        });
        if (!error.is_empty()) {
            GUI::MessageBox::show_error(window, error);
            return;
        }
        need_to_write_wav = true;
        wav_progress_window->set_filename(save_path.value());
        wav_progress_window->show();
    }));
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
        return;
    }));

    auto edit_menu = window->add_menu("&Edit"_string);
    TRY(main_widget->add_track_actions(edit_menu));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Piano"_string, app_icon, window));

    window->show();

    return app->exec();
}
