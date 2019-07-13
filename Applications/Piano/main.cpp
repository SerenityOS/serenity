#include "Music.h"
#include "PianoWidget.h"
#include <LibCore/CFile.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    CFile audio("/dev/audio");
    if (!audio.open(CIODevice::WriteOnly)) {
        dbgprintf("Can't open audio device: %s", audio.error_string());
        return 1;
    }

    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("Piano");
    window->set_rect(100, 100, 512, 512);

    auto* piano_widget = new PianoWidget;
    window->set_main_widget(piano_widget);

    window->show();

    for (;;) {
        GEventLoop::current().pump(GEventLoop::WaitMode::PollForEvents);
        u8 buffer[4096];
        piano_widget->fill_audio_buffer(buffer, sizeof(buffer));
        audio.write(buffer, sizeof(buffer));
    }

    return 0;
}
