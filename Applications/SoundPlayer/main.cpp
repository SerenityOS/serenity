#include "SoundPlayerWidget.h"
#include <AK/StringBuilder.h>
#include <LibAudio/ABuffer.h>
#include <LibAudio/AClientConnection.h>
#include <LibAudio/AWavLoader.h>
#include <LibCore/CTimer.h>
#include <LibDraw/CharacterBitmap.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GSlider.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <wav-file>\n", argv[0]);
        return 0;
    }

    GApplication app(argc, argv);

    String path = argv[1];
    AWavLoader loader(path);

    if (loader.has_error()) {
        fprintf(stderr, "Failed to load WAV file: %s (%s)\n", path.characters(), loader.error_string());
        return 1;
    }

    auto audio_client = AClientConnection::construct();
    audio_client->handshake();

    auto app_menu = make<GMenu>("SoundPlayer");
    app_menu->add_action(GCommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));

    auto menubar = make<GMenuBar>();
    menubar->add_menu(move(app_menu));
    app.set_menubar(move(menubar));

    auto window = GWindow::construct();
    window->set_title("SoundPlayer");
    window->set_resizable(false);
    window->set_rect(300, 300, 350, 140);
    window->set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/app-sound-player.png"));

    auto player = SoundPlayerWidget::construct(audio_client, loader);
    window->set_main_widget(player);
    window->show();

    return app.exec();
}
