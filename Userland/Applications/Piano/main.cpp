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

static ErrorOr<void> export_wav(StringView save_path, TrackManager& track_manager, Atomic<int>& wav_percent_written, ExportProgressWindow& wav_progress_window)
{
    Audio::WavWriter wav_writer;
    TRY(wav_writer.set_file(save_path));

    auto wav_buffer = TRY(FixedArray<DSP::Sample>::create(sample_count));

    wav_progress_window.set_filename(save_path);
    wav_progress_window.show();

    Threading::MutexLocker lock(track_manager.playback_lock());

    auto old_time = track_manager.transport()->time();
    track_manager.transport()->set_time(0);
    ScopeGuard cleanup = [&]() {
        track_manager.transport()->set_time(old_time);
        if (wav_progress_window.is_visible())
            wav_progress_window.close();
    };

    do {
        // FIXME: This progress detection is crude, but it works for now.
        wav_percent_written.store(static_cast<int>(static_cast<float>(track_manager.transport()->time()) / roll_length * 100.0f));
        track_manager.fill_buffer(wav_buffer);
        TRY(wav_writer.write_samples(wav_buffer.span()));
    } while (track_manager.transport()->time());

    TRY(wav_writer.finalize());
    wav_percent_written.store(100);
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio thread proc rpath cpath wpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(arguments));

    TrackManager track_manager;
    auto audio_loop = AudioPlayerLoop::construct(track_manager);

    auto app_icon = GUI::Icon::default_icon("app-piano"sv);
    auto window = GUI::Window::construct();
    auto main_widget = TRY(MainWidget::try_create(track_manager, audio_loop));
    window->set_main_widget(main_widget);
    window->set_title("Piano");
    window->restore_size_and_position("Piano"sv, "Window"sv, { { 840, 600 } });
    window->save_size_and_position_on_close("Piano"sv, "Window"sv);
    window->set_icon(app_icon.bitmap_for_size(16));

    Atomic<int> wav_percent_written = 0;
    auto wav_progress_window = ExportProgressWindow::construct(*window, wav_percent_written);
    TRY(wav_progress_window->initialize());

    auto main_widget_updater = Core::Timer::create_repeating(static_cast<int>((1 / 30.0) * 1000), [&] {
        if (window->is_active())
            Core::EventLoop::current().post_event(main_widget, make<Core::CustomEvent>(0));
    });
    main_widget_updater->start();

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::Action::create("Export...", { Mod_Ctrl, Key_E }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/file-export.png"sv)), [&](const GUI::Action&) {
        Optional<ByteString> save_path = GUI::FilePicker::get_save_filepath(window, "Untitled", "wav");
        if (!save_path.has_value())
            return;

        auto maybe_error = export_wav(save_path.value(), track_manager, wav_percent_written, *wav_progress_window);
        if (maybe_error.is_error())
            GUI::MessageBox::show_error(window, ByteString::formatted("Failed to export WAV file: {}", maybe_error.release_error()));
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
