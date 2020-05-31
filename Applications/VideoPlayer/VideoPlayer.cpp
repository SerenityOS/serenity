/*
 * Copyright (c) 2020, Dominic Szablewski <dominic@phoboslab.org>
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

#include "VideoPlayer.h"

#include <LibGUI/BoxLayout.h>
#include <LibGUI/MessageBox.h>
#include <LibAudio/Buffer.h>


#define PL_MPEG_IMPLEMENTATION
#include "pl_mpeg.h"

void video_callback(plm_t *plm, plm_frame_t *frame, void *user)
{
    UNUSED_PARAM(plm);
    VideoPlayer *player = (VideoPlayer *)user;
    player->on_video_decode(frame);
}

void audio_callback(plm_t *plm, plm_samples_t *samples, void *user)
{
    UNUSED_PARAM(plm);
    VideoPlayer *player = (VideoPlayer *)user;
    player->on_audio_decode(samples);
}

VideoPlayer::VideoPlayer()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    m_frame_widget = add<FrameWidget>();

    m_control_widget = add<GUI::Widget>();
    m_control_widget->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_control_widget->set_preferred_size(0, 24);
    m_control_widget->set_layout<GUI::HorizontalBoxLayout>();
    m_control_widget->layout()->set_margins({ 2, 2, 2, 2 });

    m_play = m_control_widget->add<GUI::Button>();
    m_play->set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    m_play->set_preferred_size(20, 20);
    m_play->set_icon(*m_play_icon);
    m_play->set_enabled(false);
    m_play->on_click = [this](auto) {
        m_paused = !m_paused;
        m_play->set_icon(m_paused ? *m_play_icon : *m_pause_icon);
    };

    m_current_time = m_control_widget->add<GUI::Label>();
    m_current_time->set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    m_current_time->set_preferred_size(32, 0);
    m_current_time->set_text("--:--");

    m_slider = m_control_widget->add<Slider>(Orientation::Horizontal);
    m_slider->set_min(0);
    m_slider->set_enabled(false);
    m_slider->on_change = [&](int value) {
        m_seek_msec = value;
    };
    m_slider->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_slider->set_preferred_size(0, 20);

    m_total_time = m_control_widget->add<GUI::Label>();
    m_total_time->set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    m_total_time->set_preferred_size(32, 0);
    m_total_time->set_text("--:--");


    m_audio_client = Audio::ClientConnection::construct();
    m_audio_client->handshake();

    m_plm = NULL;
    m_seek_msec = -1;
    m_paused = true;
    m_zoom = 1;
    m_last_label_time = -1;
    m_fullscreen = false;
   

    m_timer.start();
    stop_timer();
    start_timer(16);
}

VideoPlayer::~VideoPlayer()
{
    plm_destroy(m_plm);
}

VideoPlayer::Slider::~Slider()
{
}

void VideoPlayer::open_file(String path) {
    if (m_plm) {
        plm_destroy(m_plm);
        m_plm = NULL;
    }


    m_plm = plm_create_with_filename(path.characters());
    if (!m_plm || !plm_has_headers(m_plm)) {
        GUI::MessageBox::show("Selected file does not appear to be a valid \"MPEG-PS\" file!", "Filetype error", GUI::MessageBox::Type::Error);
        m_slider->set_enabled(false);
        m_play->set_enabled(false);
        return;
    }

    plm_set_loop(m_plm, TRUE);
    plm_set_audio_enabled(m_plm, TRUE);
    plm_set_audio_lead_time(m_plm, 0.01);
    plm_set_video_decode_callback(m_plm, video_callback, this);
    plm_set_audio_decode_callback(m_plm, audio_callback, this);

    int total_msec = plm_get_duration(m_plm)*1000;
    m_slider->set_max(total_msec);
    m_slider->set_step(total_msec/20);
    m_slider->set_enabled(true);

    int minutes = total_msec/60000;
    int seconds = (total_msec/1000) % 60;
    m_total_time->set_text(String::format("%02d:%02d", minutes, seconds));

    m_play->set_icon(*m_pause_icon);
    m_play->set_enabled(true);
    m_paused = false;

    m_seek_msec = -1;

    resize_to_video_dimensions();
}

void VideoPlayer::resize_to_video_dimensions()
{
    if (!m_plm)
        return;

    int width = plm_get_width(m_plm);
    int height = plm_get_height(m_plm);

    // Todo: figure out the needed window size automatically based, based on
    // all widget sizes. The controls widget height (27px) is hardcoded here.
    Gfx::Rect rect = window()->rect();
    rect.set_size(width * m_zoom, height * m_zoom + 27); 
    window()->set_rect(rect);
}

void VideoPlayer::keep_aspect_ratio(bool keep_aspect_ratio)
{
    m_frame_widget->keep_aspect_ratio(keep_aspect_ratio);
}

void VideoPlayer::zoom(float zoom)
{
    m_zoom = zoom;
    resize_to_video_dimensions();
}

void VideoPlayer::fullscreen(bool fullscreen)
{
    m_fullscreen = fullscreen;
    m_control_widget->set_visible(!fullscreen);
}

void VideoPlayer::paint_event(GUI::PaintEvent& event)
{
    UNUSED_PARAM(event);
}

void VideoPlayer::timer_event(Core::TimerEvent&)
{
    float elapsed = (float)m_timer.elapsed()/1000.0f;
    m_timer.start();

    if (!m_plm)
        return;

    // Seek
    if (m_seek_msec > 0) {
        m_audio_client->clear_buffer(false);

        plm_seek(m_plm, m_seek_msec / 1000.0f, FALSE);
        m_seek_msec = -1;
    }

    // Normal playback
    else if (!m_paused) {
        // We don't implement a frame skip here, so if the elapsed time would
        // imply a frame skip, we instead just slow down the decoder below 
        // real time.
        float seconds_per_frame = 1.0f/plm_get_framerate(m_plm);
        if (elapsed > seconds_per_frame) {
            elapsed = seconds_per_frame;
        }
        plm_decode(m_plm, elapsed);


        // Updating the slider while in fullscreen (where it should be 
        // invisible) leads to some artifacts, so don't update!
        if (!m_fullscreen)
            m_slider->set_value(plm_get_time(m_plm)*1000);
    }

    int time = plm_get_time(m_plm);
    if (time != m_last_label_time && !m_fullscreen) {
        m_last_label_time = time;
        
        int minutes = time/60;
        int seconds = time % 60;
        m_current_time->set_text(String::format("%02d:%02d", minutes, seconds));
    }  
}

void VideoPlayer::on_video_decode(plm_frame_t *frame)
{
    m_frame_widget->receive_frame(frame);
    update();
}

void VideoPlayer::on_audio_decode(plm_samples_t *samples)
{
    int output_sample_rate = 44100;
    int input_sample_rate = plm_get_samplerate(m_plm);
    float resample_ratio = (float)input_sample_rate / (float)output_sample_rate;
    int output_sample_count = samples->count / resample_ratio;

    Vector<Audio::Sample> samples_vec;
    samples_vec.ensure_capacity(output_sample_count);
    
    // Maybe add a real resampler here...
    for (int i = 0; i < output_sample_count; i++) {
        int si = (i * resample_ratio) * 2;
        float left = samples->interleaved[si];
        float right = samples->interleaved[si+1];
        samples_vec.unchecked_append(Audio::Sample(left, right));
    }

    auto buffer = Audio::Buffer::create_with_samples(move(samples_vec));
    m_audio_client->try_enqueue(buffer);
}



