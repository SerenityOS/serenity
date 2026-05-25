/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleWidget.h"

#include <AK/LexicalPath.h>
#include <AK/Math.h>
#include <AK/RefPtr.h>
#include <AK/Span.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <LibAudio/Encoder.h>
#include <LibAudio/FlacWriter.h>
#include <LibAudio/Queue.h>
#include <LibAudio/SampleFormats.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibCore/Timer.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystem/TempFile.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibThreading/Thread.h>
#include <time.h>

#include "RenderStruct.h"
#include "SampleBuffer.h"
#include "SampleEditorPalette.h"
#include "SampleFormatStruct.h"

SampleWidget::SampleWidget()
{
    clear();

    auto audio_connection_or_error = Audio::ConnectionToServer::try_create();
    if (!audio_connection_or_error.is_error()) {
        m_audio_connection = audio_connection_or_error.release_value();
    }

    m_playback_visual_timer = Core::Timer::create_repeating(playback_visual_update_rate_ms, [this]() {
        if (!m_playing)
            return;
        if (m_audio_finished.load() && m_audio_connection->remaining_buffers() == 0) {
            stop();
            if (on_playback_finished)
                on_playback_finished();
            return;
        }
        update();
    });
}

void SampleWidget::reset_view_state()
{
    set_fill_with_background_color(false);
    set_should_hide_unnecessary_scrollbars(false);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    set_scrollbars_enabled(true);
    horizontal_scrollbar().set_step(32);
    horizontal_scrollbar().set_visible(true);
    vertical_scrollbar().set_visible(false);
    horizontal_scrollbar().set_value(0);

    m_start = 0.0;
    m_scale = 1.0;
    m_painter.reset();
    m_selected = false;
    m_dragging = false;
    m_selection_start = 0;
    m_selection_end = 1;
    m_cursor_placed = false;
}

void SampleWidget::clear()
{
    reset_view_state();
    m_samples.clear();
    zoom();
}

void SampleWidget::set(NonnullRefPtr<SampleBlock> block)
{
    reset_view_state();
    m_samples.set(block);
    zoom();
}

ErrorOr<String> SampleWidget::selection()
{
    if (!m_selected) {
        return Error::from_string_literal("No selection to copy");
    }

    double start = min(m_selection_start, m_selection_end);
    double end = max(m_selection_start, m_selection_end);

    if (start == end) {
        return Error::from_string_literal("Cannot copy zero-length selection");
    }

    auto info = m_samples.selection_info(start, end);

    return String::formatted(
        "{{ \"sources\": {}, \"start\":{}, \"end\":{} }}",
        info.sources, info.adjusted_start, info.adjusted_end);
}

ErrorOr<String> SampleWidget::cut()
{
    if (!m_selected) {
        return Error::from_string_literal("No selection to cut");
    }

    double start = min(m_selection_start, m_selection_end);
    double end = max(m_selection_start, m_selection_end);

    if (start == end) {
        return Error::from_string_literal("Cannot cut zero-length selection");
    }

    auto info = TRY(m_samples.cut(start, end));

    m_selected = false;

    must_repaint();

    if (on_selection_changed)
        on_selection_changed();

    return String::formatted(
        "{{ \"sources\": {}, \"start\":{}, \"end\":{} }}",
        info.sources, info.adjusted_start, info.adjusted_end);
}

void SampleWidget::select_all()
{
    m_selection_start = 0;
    m_selection_end = 1;
    m_selected = true;
    must_repaint();

    if (on_selection_changed)
        on_selection_changed();
}

ErrorOr<void> SampleWidget::paste_from_text(String clipboard_text)
{
    bool is_initial_state = m_samples.is_empty();

    if (!m_cursor_placed && !is_initial_state) {
        return Error::from_string_literal("No cursor position set. Click to place cursor before pasting.");
    }

    double paste_position = m_cursor_placed ? m_cursor : 0.0;

    if (paste_position < 0.0 || paste_position > 1.0) {
        return Error::from_string_literal("Invalid cursor position (out of bounds)");
    }

    auto new_cursor_position = TRY(m_samples.parse_and_insert(clipboard_text, paste_position));

    m_cursor = new_cursor_position;
    m_cursor_placed = true;

    m_selected = false;

    must_repaint();

    if (on_selection_changed)
        on_selection_changed();

    return {};
}

void SampleWidget::clear_selection()
{
    m_selection_start = 0;
    m_selection_end = 1;
    m_selected = false;
    must_repaint();

    if (on_selection_changed)
        on_selection_changed();
}

void SampleWidget::zoom_in()
{
    if (m_scale < m_samples.length()) {
        m_scale *= 2;
        zoom();
    }
}

void SampleWidget::zoom_out()
{
    m_scale /= 2;
    m_scale = floor(m_scale);
    if (m_scale < 1) {
        m_scale = 1;
    }
    zoom();
}

void SampleWidget::zoom()
{
    int height = frame_inner_rect().height() - horizontal_scrollbar().height();
    int width = frame_inner_rect().width();
    auto scrollable_size = Gfx::IntSize(width * m_scale, height);
    set_content_size(scrollable_size);
    int h_pos = static_cast<int>(m_start * m_scale * static_cast<double>(width));
    horizontal_scrollbar().set_value(h_pos);
    must_repaint();
}

void SampleWidget::paint_event([[maybe_unused]] GUI::PaintEvent& event)
{
    auto result = m_painter.paint(
        *this,
        event,
        m_samples,
        {
            .selected = m_selected,
            .start = m_selection_start,
            .end = m_selection_end,
            .start_absolute = m_selection_start_absolute,
            .end_absolute = m_selection_end_absolute,
        },
        {
            .placed = m_cursor_placed,
            .position = m_cursor,
        },
        m_scale,
        current_playback_position_ratio());

    m_start = result.viewport_start;

    if (result.samples_were_unused)
        m_dragging = false;
}

void SampleWidget::mousedown_event(GUI::MouseEvent& event)
{
    double width = frame_inner_rect().width();
    int clamped_x = AK::clamp(event.position().x(), 0, static_cast<int>(width));
    m_dragging = true;
    m_drag_elapsed_timer.start();
    m_drag_elapsed_timer_active = true;
    m_cursor_placed = false;
    m_selection_start = m_selection_end = (static_cast<double>(clamped_x) / width) / m_scale + m_start;
    m_selection_start_absolute = m_selection_end_absolute = clamped_x;
    m_drag_paint_position_valid = true;
    m_last_drag_paint_absolute = m_selection_end_absolute;
    must_repaint();
}

void SampleWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_dragging)
        return;
    double width = frame_inner_rect().width();
    int clamped_x = AK::clamp(event.position().x(), 0, static_cast<int>(width));
    m_selection_end = (static_cast<double>(clamped_x) / width) / m_scale + m_start;
    m_selection_end_absolute = clamped_x;
    m_selected = true;
    constexpr int drag_refresh_interval_ms = 16;
    constexpr int minimum_unthrottled_pixels = 16;
    int selection_width = abs(m_selection_end_absolute - m_selection_start_absolute);
    auto record_paint_position = [&]() {
        m_drag_paint_position_valid = true;
        m_last_drag_paint_absolute = m_selection_end_absolute;
    };

    if (!m_drag_elapsed_timer_active) {
        m_drag_elapsed_timer.start();
        m_drag_elapsed_timer_active = true;
        drag_repaint();
        record_paint_position();
        return;
    }
    if (selection_width < minimum_unthrottled_pixels) {
        drag_repaint();
        m_drag_elapsed_timer.start();
        record_paint_position();
        return;
    }
    constexpr int drag_distance_threshold_px = 24;
    bool moved_far_since_last_paint = !m_drag_paint_position_valid || abs(m_selection_end_absolute - m_last_drag_paint_absolute) >= drag_distance_threshold_px;
    if (m_drag_elapsed_timer.elapsed() >= drag_refresh_interval_ms || moved_far_since_last_paint) {
        drag_repaint();
        m_drag_elapsed_timer.start();
        record_paint_position();
    }
}

void SampleWidget::mouseup_event(GUI::MouseEvent& event)
{
    double width = frame_inner_rect().width();
    int clamped_x = AK::clamp(event.position().x(), 0, static_cast<int>(width));
    if (m_dragging) {
        m_selection_end = (static_cast<double>(clamped_x) / width) / m_scale + m_start;
    }
    m_dragging = false;
    if (m_selection_start == m_selection_end) {
        m_cursor = m_selection_end;
        m_selection_start = 0;
        m_selection_end = 1;
        m_selected = false;
        m_cursor_placed = true;
        must_repaint();
    }

    m_drag_elapsed_timer_active = false;
    m_drag_paint_position_valid = false;

    if (on_selection_changed)
        on_selection_changed();
}

ErrorOr<void> SampleWidget::save(StringView path)
{
    auto format_or_error = m_samples.get_format();
    if (format_or_error.is_error()) {
        return Error::from_string_literal("Cannot save: no valid audio format found");
    }
    auto format = format_or_error.value();

    int const rate = format.sample_rate;
    int const channels = format.num_channels;
    int const bits = format.bits_per_sample;

    auto temp_file = TRY(FileSystem::TempFile::create_temp_file());
    auto temp_path = temp_file->path();

    auto extension = LexicalPath { path }.extension();

    auto create_writer = [&]() -> ErrorOr<NonnullOwnPtr<Audio::Encoder>> {
        if (extension.equals_ignoring_ascii_case("flac"sv)) {
            auto stream = TRY(Core::File::open(temp_path, Core::File::OpenMode::Write));
            return TRY(Audio::FlacWriter::create(move(stream), rate, channels, bits));
        } else {
            // Default to WAV for .wav or unknown extensions.
            return TRY(Audio::WavWriter::create_from_file(
                temp_path, rate, channels,
                Audio::integer_sample_format_for(bits).value()));
        }
    };

    auto writer = TRY(create_writer());

    m_samples.begin_loading_samples();
    while (true) {
        FixedArray<Audio::Sample> samples = m_samples.load_more_samples();
        if (samples.size()) {
            TRY(writer->write_samples(samples.span()));
        } else {
            TRY(writer->finalize());
            break;
        }
    };

    TRY(FileSystem::move_file(path, temp_path));

    auto source_file = TRY(try_make_ref_counted<SampleFile>(path));
    size_t length = source_file->length();
    auto file_block = TRY(try_make_ref_counted<SampleBlock>(source_file, 0, length - 1));
    set(file_block);

    return {};
}

void SampleWidget::play()
{
    if (!m_audio_connection)
        return;

    if (m_selected) {
        m_playback_start = min(m_selection_start, m_selection_end);
        m_playback_end = max(m_selection_start, m_selection_end);
    } else if (m_cursor_placed) {
        m_playback_start = m_cursor;
        m_playback_end = 1.0;
    } else {
        m_playback_start = 0.0;
        m_playback_end = 1.0;
    }

    if (m_samples.length() == 0)
        return;

    must_repaint();

    double normalized_start = min(m_playback_start, m_playback_end);
    double normalized_end = max(m_playback_start, m_playback_end);

    size_t total_samples = m_samples.length();
    size_t range_start_sample = min(total_samples, static_cast<size_t>(normalized_start * static_cast<double>(total_samples)));
    size_t range_end_sample = min(total_samples, static_cast<size_t>(normalized_end * static_cast<double>(total_samples)));
    if (total_samples > 0) {
        range_start_sample = min(range_start_sample, total_samples - 1);
        if (range_end_sample <= range_start_sample)
            range_end_sample = min(total_samples, range_start_sample + 1);
    }
    m_playback_total_sample_count = range_end_sample > range_start_sample ? range_end_sample - range_start_sample : total_samples;
    if (m_playback_total_sample_count == 0 && total_samples > 0)
        m_playback_total_sample_count = total_samples;

    m_playback_start_device_sample_index = m_audio_connection->total_played_samples();

    m_audio_connection->set_self_sample_rate(static_cast<u32>(m_samples.sample_rate()));
    m_samples.begin_loading_samples_at(m_playback_start);
    m_samples_played = 0;
    m_audio_should_stop.store(false);
    m_audio_finished.store(false);

    m_playing = true;
    m_audio_connection->async_start_playback();

    m_audio_thread = Threading::Thread::construct([this]() {
        return audio_thread_main();
    },
        "Sample playback"sv);
    m_audio_thread->start();

    if (m_playback_visual_timer)
        m_playback_visual_timer->start();
    update();
}

void SampleWidget::stop()
{
    if (!m_audio_connection)
        return;

    m_audio_should_stop.store(true);
    m_playing = false;

    if (m_audio_thread && m_audio_thread->is_started()) {
        (void)m_audio_thread->join();
        m_audio_thread = nullptr;
    }

    if (m_playback_visual_timer)
        m_playback_visual_timer->stop();

    m_playback_total_sample_count = 0;
    m_audio_connection->async_pause_playback();
    m_audio_connection->clear_client_buffer();
    m_audio_connection->async_clear_buffer();
    update();
}

intptr_t SampleWidget::audio_thread_main()
{
    double sample_rate = m_samples.sample_rate();
    double buffer_play_time_ns = 1'000'000'000.0 / (sample_rate / static_cast<double>(Audio::AUDIO_BUFFER_SIZE));
    auto sleep_time = Duration::from_nanoseconds(static_cast<i64>(buffer_play_time_ns)).to_timespec();

    Array<Audio::Sample, Audio::AUDIO_BUFFER_SIZE> chunk {};
    size_t chunk_fill = 0;
    bool ok = true;

    auto enqueue_chunk = [&]() {
        for (size_t i = chunk_fill; i < Audio::AUDIO_BUFFER_SIZE; i++)
            chunk[i] = {};
        if (m_audio_connection->blocking_realtime_enqueue(chunk, [&]() {
                                  nanosleep(&sleep_time, nullptr);
                              })
                .is_error())
            ok = false;
        chunk_fill = 0;
    };

    while (ok && !m_audio_should_stop.load()) {
        auto buffer = m_samples.load_more_samples_in_range(m_playback_start, m_playback_end, m_samples_played);
        if (buffer.is_empty()) {
            if (chunk_fill > 0)
                enqueue_chunk();
            break;
        }

        for (size_t i = 0; i < buffer.size() && ok && !m_audio_should_stop.load(); i++) {
            chunk[chunk_fill++] = buffer[i];
            if (chunk_fill == Audio::AUDIO_BUFFER_SIZE)
                enqueue_chunk();
        }
    }

    m_audio_finished.store(true);
    return 0;
}

AK::Optional<double> SampleWidget::current_playback_position_ratio()
{
    if (!m_playing)
        return {};

    double start_ratio = AK::clamp(min(m_playback_start, m_playback_end), 0.0, 1.0);
    double end_ratio = AK::clamp(max(m_playback_start, m_playback_end), 0.0, 1.0);
    double range_ratio = end_ratio - start_ratio;

    if (range_ratio <= 0.0 || m_playback_total_sample_count == 0 || !m_audio_connection)
        return start_ratio;

    auto total_played_samples = static_cast<u64>(m_audio_connection->total_played_samples());
    u64 samples_since_start = 0;
    if (total_played_samples >= m_playback_start_device_sample_index)
        samples_since_start = total_played_samples - m_playback_start_device_sample_index;

    double progress = static_cast<double>(samples_since_start) / static_cast<double>(m_playback_total_sample_count);
    progress = AK::clamp(progress, 0.0, 1.0);

    return start_ratio + (range_ratio * progress);
}
