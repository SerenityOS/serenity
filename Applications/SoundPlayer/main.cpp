#include "SoundPlayerWidget.h"
#include <LibAudio/AClientConnection.h>
#include <LibDraw/CharacterBitmap.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GAboutDialog.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto audio_client = AClientConnection::construct();
    audio_client->handshake();

    auto window = GWindow::construct();
    window->set_title("SoundPlayer");
    window->set_resizable(false);
    window->set_rect(300, 300, 350, 140);
    window->set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/app-sound-player.png"));

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("SoundPlayer");
    auto player = SoundPlayerWidget::construct(window, audio_client);

    if (argc > 1) {
        String path = argv[1];
        player->open_file(path);
        player->manager().play();
    }

    app_menu->add_action(GCommonActions::make_open_action([&](auto&) {
        Optional<String> path = GFilePicker::get_open_filepath("Open wav file...");
        if (path.has_value()) {
            player->open_file(path.value());
        }
    }));

    app_menu->add_separator();
    app_menu->add_action(GCommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));

    menubar->add_menu(move(app_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [](auto&) {
        GAboutDialog::show("SoundPlayer", GraphicsBitmap::load_from_file("/res/icons/32x32/app-sound-player.png"));
    }));
    menubar->add_menu(move(help_menu));
    app.set_menubar(move(menubar));

    window->set_main_widget(player);
    window->show();

    return app.exec();
}
