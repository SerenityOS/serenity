#include "Music.h"
#include "PianoWidget.h"
#include <LibCore/CFile.h>
#include <LibCore/CNotifier.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GWindow.h>

static int s_pipefds[2];

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    pipe(s_pipefds);

    auto* window = new GWindow;
    window->set_title("Piano");
    window->set_rect(100, 100, 512, 512);

    auto* piano_widget = new PianoWidget;
    window->set_main_widget(piano_widget);

    window->show();

    CNotifier notifier(s_pipefds[0], CNotifier::Read);
    notifier.on_ready_to_read = [&] {
        char buffer[32];
        read(s_pipefds[1], buffer, sizeof(buffer));
        piano_widget->update();
    };

    create_thread([](void* context) -> int {
        auto* piano_widget = (PianoWidget*)context;

        CFile audio("/dev/audio");
        if (!audio.open(CIODevice::WriteOnly)) {
            dbgprintf("Can't open audio device: %s", audio.error_string());
            return 1;
        }

        for (;;) {
            u8 buffer[4096];
            piano_widget->fill_audio_buffer(buffer, sizeof(buffer));
            audio.write(buffer, sizeof(buffer));
            char ch = '!';
            write(s_pipefds[0], &ch, 1);
        }
    }, piano_widget);

    return app.exec();
}
