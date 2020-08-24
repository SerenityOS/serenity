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

#include <LibAudio/ClientConnection.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

class AudioWidget final : public GUI::Widget {
    C_OBJECT(AudioWidget)
public:
    AudioWidget()
        : m_audio_client(Audio::ClientConnection::construct())
    {
        m_audio_client->on_muted_state_change = [this](bool muted) {
            if (m_audio_muted == muted)
                return;
            m_mute_box->set_checked(!m_audio_muted);
            m_slider->set_enabled(!muted);
            m_audio_muted = muted;
            update();
        };

        m_audio_client->on_main_mix_volume_change = [this](int volume) {
            m_audio_volume = volume;
            if (!m_audio_muted)
                update();
        };

        m_volume_level_bitmaps.append({ 66, Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-high.png") });
        m_volume_level_bitmaps.append({ 33, Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-medium.png") });
        m_volume_level_bitmaps.append({ 1, Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-low.png") });
        m_volume_level_bitmaps.append({ 0, Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-zero.png") });
        m_volume_level_bitmaps.append({ 0, Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-muted.png") });

        m_slider_window = add<GUI::Window>(window());
        m_slider_window->set_frameless(true);
        m_slider_window->set_resizable(false);
        m_slider_window->set_minimizable(false);
        m_slider_window->on_active_input_change = [this](bool is_active_input) {
            if (!is_active_input)
                close();
        };

        m_root_container = m_slider_window->set_main_widget<GUI::Label>();
        m_root_container->set_fill_with_background_color(true);
        m_root_container->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
        m_root_container->set_layout<GUI::VerticalBoxLayout>();
        m_root_container->layout()->set_margins({ 0, 4, 0, 4 });
        m_root_container->layout()->set_spacing(0);
        m_root_container->set_frame_thickness(2);
        m_root_container->set_frame_shape(Gfx::FrameShape::Container);
        m_root_container->set_frame_shadow(Gfx::FrameShadow::Raised);

        m_percent_box = m_root_container->add<GUI::CheckBox>("\xE2\x84\xB9");
        m_percent_box->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        m_percent_box->set_preferred_size(27, 16);
        m_percent_box->set_checked(false);
        m_percent_box->set_tooltip("Show percent");
        m_percent_box->on_checked = [&](bool show_percent) {
            m_show_percent = show_percent;
            if (!m_show_percent) {
                window()->resize(16, 16);
                m_percent_box->set_tooltip("Show percent");
            } else {
                window()->resize(44, 16);
                m_percent_box->set_tooltip("Hide percent");
            }
            reposition_slider_window();
            GUI::Application::the()->hide_tooltip();
        };

        m_slider = m_root_container->add<GUI::VerticalSlider>();
        m_slider->set_max(20);
        m_slider->set_value(0);
        m_slider->set_knob_size_mode(GUI::Slider::KnobSizeMode::Proportional);
        m_slider->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
        m_slider->on_value_changed = [&](int value) {
            int volume = clamp((20 - value) * 5, 0, 100);
            m_audio_client->set_main_mix_volume(volume);
            update();
        };

        m_mute_box = m_root_container->add<GUI::CheckBox>("\xE2\x9D\x8C");
        m_mute_box->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        m_mute_box->set_preferred_size(27, 16);
        m_mute_box->set_checked(false);
        m_mute_box->set_tooltip("Mute");
        m_mute_box->on_checked = [&](bool is_muted) {
            m_mute_box->set_tooltip(is_muted ? "Unmute" : "Mute");
            m_audio_client->set_muted(is_muted);
            GUI::Application::the()->hide_tooltip();
        };
    }

    virtual ~AudioWidget() override { }

private:
    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Left) {
            if (!m_slider_window->is_visible())
                open();
            else
                close();
            return;
        }
        if (event.button() == GUI::MouseButton::Right) {
            m_audio_client->set_muted(!m_audio_muted);
            update();
        }
    }

    virtual void mousewheel_event(GUI::MouseEvent& event) override
    {
        if (m_audio_muted)
            return;
        int volume = clamp(m_audio_volume - event.wheel_delta() * 5, 0, 100);
        m_audio_client->set_main_mix_volume(volume);
        m_slider->set_value(20 - (volume / 5));
        update();
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.clear_rect(event.rect(), Color::from_rgba(0));

        auto& audio_bitmap = choose_bitmap_from_volume();
        painter.blit({}, audio_bitmap, audio_bitmap.rect());

        if (m_show_percent) {
            auto volume_text = m_audio_muted ? "mute" : String::format("%d%%", m_audio_volume);
            painter.draw_text({ 16, 3, 24, 16 }, volume_text, Gfx::Font::default_fixed_width_font(), Gfx::TextAlignment::TopLeft, palette().window_text());
        }
    }

    void open()
    {
        reposition_slider_window();
        m_slider_window->show();
    }

    void close()
    {
        m_slider_window->hide();
    }

    Gfx::Bitmap& choose_bitmap_from_volume()
    {
        if (m_audio_muted)
            return *m_volume_level_bitmaps.last().bitmap;

        for (auto& pair : m_volume_level_bitmaps) {
            if (m_audio_volume >= pair.volume_threshold)
                return *pair.bitmap;
        }
        ASSERT_NOT_REACHED();
    }

    void reposition_slider_window() { m_slider_window->set_rect(window()->rect_in_menubar().x() - 20, 19, 50, 100); }

    struct VolumeBitmapPair {
        int volume_threshold { 0 };
        RefPtr<Gfx::Bitmap> bitmap;
    };

    NonnullRefPtr<Audio::ClientConnection> m_audio_client;
    Vector<VolumeBitmapPair, 5> m_volume_level_bitmaps;
    bool m_show_percent { false };
    bool m_audio_muted { false };
    int m_audio_volume { 100 };

    RefPtr<GUI::Slider> m_slider;
    RefPtr<GUI::Window> m_slider_window;
    RefPtr<GUI::CheckBox> m_mute_box;
    RefPtr<GUI::CheckBox> m_percent_box;
    RefPtr<GUI::Label> m_root_container;
};

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_has_alpha_channel(true);
    window->set_title("Audio");
    window->set_window_type(GUI::WindowType::MenuApplet);
    window->resize(16, 16);

    window->set_main_widget<AudioWidget>();
    window->show();

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return app->exec();
}
