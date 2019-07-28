#include "Music.h"
#include "PianoWidget.h"
#include <LibAudio/AClientConnection.h>
#include <LibCore/CFile.h>
#include <LibCore/CThread.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);
    AClientConnection audio_connection;
    audio_connection.handshake();

    auto* window = new GWindow;
    window->set_title("Piano");
    window->set_rect(100, 100, 512, 512);

    auto* piano_widget = new PianoWidget;
    window->set_main_widget(piano_widget);
    window->show();
    window->set_icon(load_png("/res/icons/16x16/app-piano.png"));

    CThread sound_thread([](void* context) -> int {
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
            GEventLoop::current().post_event(*piano_widget, make<CCustomEvent>(0));
            GEventLoop::current().wake();
        }
    },
        piano_widget);

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("Piano");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [](const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
