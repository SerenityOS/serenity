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
    if (pledge("stdio shared_buffer rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio shared_buffer rpath unix", nullptr) < 0) {
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
    return app.exec();
}
