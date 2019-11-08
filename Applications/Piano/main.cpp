#include "Music.h"
#include "PianoWidget.h"
#include <LibAudio/AClientConnection.h>
#include <LibCore/CFile.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <LibThread/Thread.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);
    auto audio_client = AClientConnection::construct();
    audio_client->handshake();

    auto window = GWindow::construct();
    window->set_title("Piano");
    window->set_rect(100, 100, 512, 512);

    auto piano_widget = PianoWidget::construct();
    window->set_main_widget(piano_widget);
    window->show();
    window->set_icon(load_png("/res/icons/16x16/app-piano.png"));

    LibThread::Thread sound_thread([piano_widget = piano_widget.ptr()] {
        auto audio = CFile::construct("/dev/audio");
        if (!audio->open(CIODevice::WriteOnly)) {
            dbgprintf("Can't open audio device: %s", audio->error_string());
            return 1;
        }

        for (;;) {
            u8 buffer[4096];
            piano_widget->fill_audio_buffer(buffer, sizeof(buffer));
            audio->write(buffer, sizeof(buffer));
            CEventLoop::current().post_event(*piano_widget, make<CCustomEvent>(0));
            CEventLoop::wake();
        }
    });
    sound_thread.start();

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("Piano");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
