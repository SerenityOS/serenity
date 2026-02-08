/*
 * Copyright (c) 2022-2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleWidget.h"

#include <AK/LexicalPath.h>
#include <AK/Math.h>
#include <AK/RefPtr.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <LibAudio/Encoder.h>
#include <LibAudio/FlacWriter.h>
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

#include "RenderStruct.h"
#include "SampleBlockContainer.h"
#include "SampleBuffer.h"
#include "SampleEditorPalette.h"
#include "SampleFileBlock.h"
#include "SampleFormatStruct.h"
#include "SampleRenderer.h"

static double emphasized_amplitude(double amplitude)
{
    double clamped = AK::clamp(amplitude, 0.0, 1.0);
    return (4.0 / 3.0) * clamped * clamped * clamped - 3.0 * clamped * clamped + (8.0 / 3.0) * clamped;
}

SampleWidget::SampleWidget()
{
    clear();

    // Initialize audio playback
    auto audio_connection_or_error = Audio::ConnectionToServer::try_create();
    if (!audio_connection_or_error.is_error()) {
        m_audio_connection = audio_connection_or_error.release_value();
        m_playback_timer = Core::Timer::create_repeating(playback_update_rate_ms, [this]() {
            next_audio_buffer();
            if (m_playing)
                update();
        });
        MUST(m_current_audio_buffer.create(0));
    }

    m_playback_visual_timer = Core::Timer::create_repeating(playback_visual_update_rate_ms, [this]() {
        if (!m_playing)
            return;
        update();
    });
}

void SampleWidget::clear()
{
    size_t null_length = 16 * 1024;
    auto null_block = SampleBlock::create_null(
        null_length, null_length / 44100.0);

    set(null_block);
}

void SampleWidget::set(NonnullRefPtr<SampleBlock> block)
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
    m_previous_width = -1;
    m_previous_scale = -1;
    m_previous_start = -1;
    m_selected = false;
    m_dragging = false;
    m_selection_start = 0;
    m_selection_end = 1;
    m_cursor_placed = false;

    m_samples.set(block);
    zoom();
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
    int h_pos = (m_start * m_scale * ((double)width));
    horizontal_scrollbar().set_value(h_pos);
    must_repaint();
}

void SampleWidget::draw_timeline(
    GUI::Painter painter,
    Gfx::Rect<int> frame,
    SampleEditorPalette& colors,
    int offset,
    double duration,
    double h_pos,
    double width)
{

    auto timeline_rect = frame;
    timeline_rect.set_height(offset);
    painter.fill_rect(timeline_rect, colors.timeline_background_color);

    double duration_on_view = duration / m_scale;
    double start_seconds = h_pos / width * duration / m_scale;
    double mag = floor(log10(duration_on_view));
    double tick = pow(10, mag);
    double first_tick = start_seconds - fmod(start_seconds, tick);

    for (double t = first_tick; t < start_seconds + duration_on_view; t += tick) {
        for (double t2 = t; t2 < t + tick; t2 += (tick / 10)) {
            double x2 = (t2 - start_seconds) / duration_on_view * (double)width;
            Gfx::IntPoint sub_mark_top = { (int)x2, offset - (offset / 8) };
            Gfx::IntPoint sub_mark_bottom = { (int)x2, offset };
            painter.draw_line(sub_mark_top, sub_mark_bottom, colors.timeline_sub_mark_color);
        }

        double x = (t - start_seconds) / duration_on_view * (double)width;

        Gfx::IntPoint mark_top = { (int)x, offset / 2 };
        Gfx::IntPoint mark_bottom = { (int)x, offset };
        painter.draw_line(mark_top, mark_bottom, colors.timeline_main_mark_color);

        Gfx::IntRect text_rec = { (int)x + 3, (offset / 2) - (offset / 8),
            (offset / 2), (offset / 2) };

        StringBuilder time;

        if (mag < 0) {
            StringBuilder format;
            format.appendff("{{:.{}}}", mag * -1);
            time.appendff(format.string_view(), t + (tick / 2));
        } else {
            time.appendff("{}", t);
        }

        painter.draw_text(text_rec, time.string_view(), Gfx::TextAlignment::TopLeft,
            colors.black);
    }
}

void SampleWidget::paint_event([[maybe_unused]] GUI::PaintEvent& event)
{

    GUI::Painter real_painter(*this);

    RefPtr<Gfx::Bitmap> old_bitmap;
    if (m_has_bitmap) {
        auto clone_or_error = m_bitmap->clone();
        if (!clone_or_error.is_error())
            old_bitmap = clone_or_error.release_value();
    }

    auto colors = SampleEditorPalette(palette());
    double duration = m_samples.duration();

    int const offset = 16;

    double width = frame_inner_rect().width();
    double height = (double)content_rect().height() - offset - horizontal_scrollbar().height();
    int h_pos = horizontal_scrollbar().value();
    m_start = (double)h_pos / (double)width / m_scale;

    if (!m_samples.used())
        m_dragging = false;

    auto rect = frame_inner_rect();
    auto x = rect.x();
    auto y = rect.y();
    auto w = rect.width();
    auto h = rect.height();

    int sample_diff = 0;
    int start_sample = 0;
    int end_sample = width;
    bool changed = false;
    bool full_redraw = false;

    if (!m_drag_redraw) {

        if (!(width == m_previous_width && height == m_previous_height && m_previous_scale == m_scale && m_previous_start == m_start && m_samples.used())) {

            full_redraw = true;
            changed = true;

            if (width == m_previous_width && height == m_previous_height && m_scale == m_previous_scale) {

                full_redraw = false;
                sample_diff = ((m_start - m_previous_start) * m_scale * width);

                if ((sample_diff > 0 && sample_diff > width) || (sample_diff < 0 && -sample_diff > width)) {
                    full_redraw = true;

                } else if (sample_diff > 0) {
                    start_sample = width - sample_diff;
                    end_sample = width;

                } else if (sample_diff < 0) {
                    start_sample = 0;
                    end_sample = -sample_diff;

                } else {
                    changed = false;
                }
            }
        }

        m_renderer = make_ref_counted<SampleRenderer>(m_samples, width, m_start, m_scale, start_sample, end_sample);
        m_samples.set_used();

        int selection_start = width * m_scale * (m_selection_start - m_start);
        int selection_end = width * m_scale * (m_selection_end - m_start);
        int cursor = width * m_scale * (m_cursor - m_start);

        if (selection_start > selection_end + 1) {
            auto temp = selection_end;
            selection_end = selection_start;
            selection_start = temp;
        }

        double half = height / 2;

        auto paint_bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, frame_inner_rect().size());
        auto paint_bitmap = paint_bitmap_or_error.value();
        GUI::Painter painter(*paint_bitmap);

        auto bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, frame_inner_rect().size());
        m_bitmap = bitmap_or_error.value();
        GUI::Painter composite_painter(*m_bitmap);

        painter.fill_rect(frame_inner_rect(), colors.window_color);
        Gfx::IntPoint left = { 0, (int)half + offset };
        Gfx::IntPoint right = { (int)width, (int)half + offset };
        int top_y = (int)frame_inner_rect().y();
        int bottom_y = (int)frame_inner_rect().height() - frame_inner_rect().y() - horizontal_scrollbar().height();

        draw_timeline(painter, frame_inner_rect(), colors, offset, duration, h_pos, width);

        if (changed || full_redraw || m_must_redraw) {

            for (int sample = start_sample; sample < end_sample; sample++) {

                Gfx::Color waveform_light = colors.light_blue;
                Gfx::Color waveform_dark = colors.dark_blue;

                Gfx::IntPoint top = { sample, top_y + offset };
                Gfx::IntPoint bottom = { sample, bottom_y + offset };
                Gfx::IntPoint timeline_top = { sample, 0 };
                Gfx::IntPoint timeline_bottom = { sample, offset };

                if (m_selected && (sample >= selection_start) && (sample <= selection_end)) {
                    painter.draw_line(top, bottom, colors.selection_color);
                    painter.draw_line(timeline_top, timeline_bottom,
                        colors.timeline_selection_color);
                    waveform_light = colors.light_gray.lightened();
                    waveform_dark = colors.dark_gray.lightened();
                }

                if (m_cursor_placed && (sample == cursor)) {
                    painter.draw_line(top, bottom, colors.cursor_color);
                    painter.draw_line(timeline_top, timeline_bottom,
                        colors.timeline_cursor_color);
                }

                RenderStruct value = m_renderer->rendered_sample_at(sample);
                auto emphasized_peak_plus = emphasized_amplitude(value.peak_plus);
                auto emphasized_peak_minus = emphasized_amplitude(value.peak_minus);

                double y_max = half * (1 + (emphasized_peak_plus * 0.9));
                double y_min = half * (1 - (emphasized_peak_minus * 0.9));

                Gfx::IntPoint min_point = { sample, (int)y_max + offset };
                Gfx::IntPoint max_point = { sample, (int)y_min + offset };

                painter.draw_line(min_point, max_point, waveform_dark);

                auto emphasized_rms_plus = emphasized_amplitude(value.RMS_plus);
                auto emphasized_rms_minus = emphasized_amplitude(value.RMS_minus);
                y_max = half * (1 + (emphasized_rms_plus * 0.45));
                y_min = half * (1 - (emphasized_rms_minus * 0.45));

                min_point = { sample, (int)y_max + offset };
                max_point = { sample, (int)y_min + offset };

                painter.draw_line(min_point, max_point, waveform_light);
            }

            painter.draw_line(left, right, colors.dark_blue);
        }

        if (full_redraw || !m_has_bitmap || m_must_redraw) {

            composite_painter.blit(frame_inner_rect().top_left(), *paint_bitmap, frame_inner_rect());
            real_painter.blit(frame_inner_rect().top_left(), *m_bitmap, frame_inner_rect());
            m_has_bitmap = true;

        } else if (changed) {

            Gfx::Point<int> old_dest;
            Gfx::Rect<int> old_source;
            Gfx::Point<int> new_dest;
            Gfx::Rect<int> new_source;

            if (sample_diff < 0) {
                old_dest = Gfx::Point<int>(-sample_diff, y);
                old_source = Gfx::Rect(x, y, w + sample_diff, h);
                new_dest = Gfx::Point<int>(x, y);
                new_source = Gfx::Rect(x, y, -sample_diff, h);

            } else {
                old_dest = Gfx::Point<int>(x, y);
                old_source = Gfx::Rect(sample_diff, y, w - sample_diff, h);
                new_dest = Gfx::Point<int>(x + w - sample_diff, y);
                new_source = Gfx::Rect(x + w - sample_diff, y, sample_diff, h);
            }

            composite_painter.blit(new_dest, *paint_bitmap, new_source);

            composite_painter.blit(old_dest, *old_bitmap, old_source);

            real_painter.blit(frame_inner_rect().top_left(), *m_bitmap, frame_inner_rect());

            m_has_bitmap = true;

        } else if (m_has_bitmap) {

            composite_painter.blit(frame_inner_rect().top_left(), *old_bitmap, frame_inner_rect());
            real_painter.blit(frame_inner_rect().top_left(), *m_bitmap, frame_inner_rect());
        }

    } else {

        if (m_has_bitmap) {

            int start = min(m_selection_start_absolute, m_selection_end_absolute);
            int end = max(m_selection_start_absolute, m_selection_end_absolute);

            if (start > 0) {
                auto left_dest = Gfx::Point<int>(0, y);
                auto left_source = Gfx::Rect<int>(0, y, start - 1, h);
                real_painter.blit(left_dest, *m_bitmap, left_source);
            }

            if (end < width - 1) {
                auto right_dest = Gfx::Point<int>(end, y);
                auto right_source = Gfx::Rect<int>(end, y, width - end - 1, h);
                real_painter.blit(right_dest, *m_bitmap, right_source);
            }

            auto dest = Gfx::Point<int>(start, y);
            auto source = Gfx::Rect<int>(start, y, end - start, h);

            real_painter.blit_dimmed(dest, *m_bitmap, source);
        }
    }

    m_must_redraw = false;
    m_drag_redraw = false;

    m_previous_width = width;
    m_previous_height = height;
    m_previous_scale = m_scale;
    m_previous_start = m_start;

    if (!m_timer) {
        m_timer = Core::Timer::create_single_shot(25, [&]() {
            m_awaiting_repaint = false;
            must_repaint();
        });
    }

    if (changed) {
        m_awaiting_repaint = true;
        m_timer->restart();
    }

    if (!m_drag_redraw) {
        if (auto playback_ratio = current_playback_position_ratio(); playback_ratio.has_value() && m_scale != 0.0) {
            double playback_position = playback_ratio.value();
            double visible_start = m_start;
            double visible_end = m_start + (1.0 / m_scale);
            if (playback_position >= visible_start && playback_position <= visible_end) {
                int playback_cursor = AK::round_to<int>(width * m_scale * (playback_position - m_start));
                if (playback_cursor >= 0 && playback_cursor < (int)width) {
                    int absolute_x = frame_inner_rect().x() + playback_cursor;
                    int inner_top = frame_inner_rect().y();
                    int inner_bottom = frame_inner_rect().y() + frame_inner_rect().height();
                    int scrollbar_height = horizontal_scrollbar().is_visible() ? horizontal_scrollbar().height() : 0;
                    int timeline_top_y = inner_top;
                    int timeline_bottom_y = min(inner_top + offset, inner_bottom);
                    int waveform_top_y = timeline_bottom_y;
                    int waveform_bottom_y = inner_bottom - scrollbar_height;
                    waveform_bottom_y = max(waveform_top_y, waveform_bottom_y);

                    if (timeline_bottom_y > timeline_top_y)
                        real_painter.draw_line({ absolute_x, timeline_top_y }, { absolute_x, timeline_bottom_y }, colors.timeline_cursor_color);
                    if (waveform_bottom_y > waveform_top_y)
                        real_painter.draw_line({ absolute_x, waveform_top_y }, { absolute_x, waveform_bottom_y }, colors.cursor_color);
                }
            }
        }
    }
}

void SampleWidget::mousedown_event(GUI::MouseEvent& event)
{
    double width = frame_inner_rect().width();
    int clamped_x = AK::clamp(event.position().x(), 0, (int)width);
    m_dragging = true;
    m_drag_elapsed_timer.start();
    m_drag_elapsed_timer_active = true;
    m_cursor_placed = false;
    m_selection_start = m_selection_end = ((double)clamped_x / width) / m_scale + m_start;
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
    int clamped_x = AK::clamp(event.position().x(), 0, (int)width);
    m_selection_end = ((double)clamped_x / width) / m_scale + m_start;
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
    int clamped_x = AK::clamp(event.position().x(), 0, (int)width);
    if (m_dragging) {
        m_selection_end = ((double)clamped_x / width) / m_scale + m_start;
    }
    m_dragging = false;
    if (m_selection_start == m_selection_end) {
        m_cursor = m_selection_end;
        m_selection_start = 0;
        m_selection_end = 1;
        m_selected = 0;
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
            // Default to WAV for .wav or unknown extensions
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

    auto source_file = TRY(try_make_ref_counted<SampleSourceFile>(path));
    size_t length = source_file->length();
    auto file_block = TRY(try_make_ref_counted<SampleFileBlock>(
        source_file, (size_t)0, (size_t)length - 1));
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
    if (m_audio_connection)
        m_playback_start_device_sample_index = m_audio_connection->total_played_samples();
    else
        m_playback_start_device_sample_index = 0;

    double sample_rate = m_samples.sample_rate();
    m_audio_connection->set_self_sample_rate(sample_rate);
    m_samples_to_load_per_buffer = buffer_size_ms / 1000.0 * sample_rate;

    m_samples.begin_loading_samples_at(m_playback_start);
    m_samples_played = 0;
    m_finished_loading = false;

    m_audio_connection->clear_client_buffer();
    m_audio_connection->async_clear_buffer();

    m_playing = true;
    m_audio_connection->async_start_playback();
    m_playback_timer->start();
    if (m_playback_visual_timer)
        m_playback_visual_timer->start();
    update();
}

void SampleWidget::stop()
{
    if (!m_audio_connection)
        return;

    m_playing = false;
    m_playback_timer->stop();
    if (m_playback_visual_timer)
        m_playback_visual_timer->stop();
    m_playback_total_sample_count = 0;
    m_audio_connection->async_pause_playback();
    m_audio_connection->clear_client_buffer();
    m_audio_connection->async_clear_buffer();
    update();
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

void SampleWidget::next_audio_buffer()
{
    if (!m_playing || !m_audio_connection)
        return;

    if (m_finished_loading) {
        if (m_audio_connection->remaining_samples() == 0) {
            stop();
            if (on_playback_finished)
                on_playback_finished();
        }
        return;
    }

    while (m_audio_connection->remaining_samples() < m_samples_to_load_per_buffer * always_enqueued_buffer_count) {
        auto buffer = m_samples.load_more_samples_in_range(m_playback_start, m_playback_end, m_samples_played);

        if (buffer.size() == 0) {
            m_finished_loading = true;
            return;
        }

        m_current_audio_buffer.swap(buffer);
        MUST(m_audio_connection->async_enqueue(m_current_audio_buffer));
    }
}
