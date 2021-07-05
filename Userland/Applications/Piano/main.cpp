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
#include <AK/Queue.h>
#include <LibAudio/Buffer.h>
#include <LibAudio/ClientConnection.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/Object.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibThreading/Thread.h>

// Converts Piano-internal data to an Audio::Buffer that AudioServer receives
static NonnullRefPtr<Audio::Buffer> music_samples_to_buffer(Array<Sample, sample_count> samples)
{
    Vector<Audio::Frame, sample_count> frames;
    frames.ensure_capacity(sample_count);
    for (auto sample : samples) {
        Audio::Frame frame = { sample.left / (double)NumericLimits<i16>::max(), sample.right / (double)NumericLimits<i16>::max() };
        frames.unchecked_append(frame);
    }
    return Audio::Buffer::create_with_samples(frames);
}

// Wrapper class accepting custom events to advance the track playing and forward audio data to the system.
// This does not run on a separate thread, preventing IPC multithreading madness.
class AudioPlayerLoop : public Core::Object {
    C_OBJECT(AudioPlayerLoop)
public:
    AudioPlayerLoop(TrackManager& track_manager, bool& need_to_write_wav, Audio::WavWriter& wav_writer)
        : m_track_manager(track_manager)
        , m_need_to_write_wav(need_to_write_wav)
        , m_wav_writer(wav_writer)
    {
        m_audio_client = Audio::ClientConnection::construct();
        m_audio_client->on_finish_playing_buffer = [this](int buffer_id) {
            (void)buffer_id;
            enqueue_audio();
        };
    }

    void enqueue_audio()
    {
        m_track_manager.fill_buffer(m_buffer);
        NonnullRefPtr<Audio::Buffer> audio_buffer = music_samples_to_buffer(m_buffer);
        m_audio_client->async_enqueue(audio_buffer);

        // FIXME: This should be done somewhere else.
        if (m_need_to_write_wav) {
            m_need_to_write_wav = false;
            m_track_manager.reset();
            m_track_manager.set_should_loop(false);
            do {
                m_track_manager.fill_buffer(m_buffer);
                m_wav_writer.write_samples(reinterpret_cast<u8*>(m_buffer.data()), buffer_size);
            } while (m_track_manager.time());
            m_track_manager.reset();
            m_track_manager.set_should_loop(true);
            m_wav_writer.finalize();
        }
    }

private:
    TrackManager& m_track_manager;
    Array<Sample, sample_count> m_buffer;
    RefPtr<Audio::ClientConnection> m_audio_client;

    bool& m_need_to_write_wav;
    Audio::WavWriter& m_wav_writer;
};

int main(int argc, char** argv)
{
    if (pledge("stdio thread rpath cpath wpath recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

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

    auto audio_loop = AudioPlayerLoop::construct(track_manager, need_to_write_wav, wav_writer);
    audio_loop->enqueue_audio();
    audio_loop->enqueue_audio();

    auto main_widget_updater = Core::Timer::construct(static_cast<int>((1 / 60.0) * 1000), [&] {
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
