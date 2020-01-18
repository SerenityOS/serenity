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

#include <LibAudio/AClientConnection.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class AudioWidget final : public GWidget {
    C_OBJECT(AudioWidget)
public:
    AudioWidget()
        : GWidget(nullptr)
    {
        m_audio_client = make<AClientConnection>();
        m_audio_client->on_muted_state_change = [this](bool muted) {
            if (m_audio_muted == muted)
                return;
            m_audio_muted = muted;
            update();
        };
        m_unmuted_bitmap = GraphicsBitmap::load_from_file("/res/icons/audio-unmuted.png");
        m_muted_bitmap = GraphicsBitmap::load_from_file("/res/icons/audio-muted.png");
    }

    virtual ~AudioWidget() override {}

private:
    virtual void mousedown_event(GMouseEvent& event) override
    {
        if (event.button() != GMouseButton::Left)
            return;
        m_audio_client->set_muted(!m_audio_muted);
        update();
    }

    virtual void paint_event(GPaintEvent& event) override
    {
        GPainter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.clear_rect(event.rect(), Color::from_rgba(0));
        auto& audio_bitmap = m_audio_muted ? *m_muted_bitmap : *m_unmuted_bitmap;
        painter.blit({}, audio_bitmap, audio_bitmap.rect());
    }

    OwnPtr<AClientConnection> m_audio_client;
    RefPtr<GraphicsBitmap> m_muted_bitmap;
    RefPtr<GraphicsBitmap> m_unmuted_bitmap;
    bool m_audio_muted { false };
};

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio shared_buffer accept rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GWindow::construct();
    window->set_has_alpha_channel(true);
    window->set_window_type(GWindowType::MenuApplet);
    window->resize(12, 16);

    auto widget = AudioWidget::construct();
    window->set_main_widget(widget);
    window->show();

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return app.exec();
}
