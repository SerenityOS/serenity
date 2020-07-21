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
#include <LibGUI/Painter.h>
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
            m_audio_muted = muted;
            update();
        };

        m_audio_client->on_main_mix_volume_change = [this](int volume) {
            m_audio_volume = volume;
            if (!m_audio_muted)
                update();
        };

        m_volume_level_bitmaps.append({66, Gfx::Bitmap::load_from_file("/res/icons/audio-2.png")});
        m_volume_level_bitmaps.append({33, Gfx::Bitmap::load_from_file("/res/icons/audio-1.png")});
        m_volume_level_bitmaps.append({1,  Gfx::Bitmap::load_from_file("/res/icons/audio-0.png")});
        m_volume_level_bitmaps.append({0,  Gfx::Bitmap::load_from_file("/res/icons/audio-muted.png")});
    }

    virtual ~AudioWidget() override {}

private:
    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() != GUI::MouseButton::Left)
            return;
        m_audio_client->set_muted(!m_audio_muted);
        update();
    }

    virtual void mousewheel_event(GUI::MouseEvent& event) override
    {
        if (m_audio_muted)
            return;
        int volume = clamp(m_audio_volume - event.wheel_delta() * 5, 0, 100);
        m_audio_client->set_main_mix_volume(volume);
        update();
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.clear_rect(event.rect(), Color::from_rgba(0));

        auto& audio_bitmap = choose_bitmap_from_volume();
        painter.blit({}, audio_bitmap, audio_bitmap.rect());

        auto volume_text = m_audio_muted ? "Mut" : String::format("%d%%", m_audio_volume);
        painter.draw_text({16, 3, 24, 16}, volume_text, Gfx::Font::default_font(), Gfx::TextAlignment::TopLeft, palette().window_text());
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

    struct VolumeBitmapPair {
        int volume_threshold { 0 };
        RefPtr<Gfx::Bitmap> bitmap;
    };

    NonnullRefPtr<Audio::ClientConnection> m_audio_client;
    Vector<VolumeBitmapPair, 4> m_volume_level_bitmaps;
    bool m_audio_muted { false };
    int m_audio_volume { 100 };
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
    window->resize(42, 16);

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
