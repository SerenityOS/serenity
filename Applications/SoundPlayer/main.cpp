#include "SoundPlayerWidget.h"
#include <LibAudio/AClientConnection.h>
#include <LibDraw/CharacterBitmap.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

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

    auto audio_client = AClientConnection::construct();
    audio_client->handshake();

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GWindow::construct();
    window->set_title("SoundPlayer");
    window->set_resizable(false);
    window->set_rect(300, 300, 350, 140);
    window->set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/app-sound-player.png"));

    auto menubar = make<GMenuBar>();
    auto app_menu = GMenu::construct("SoundPlayer");
    auto player = SoundPlayerWidget::construct(window, audio_client);

    if (argc > 1) {
        String path = argv[1];
        player->open_file(path);
        player->manager().play();
    }

    auto hide_scope = GAction::create("Hide scope", { Mod_Ctrl, Key_H }, [&](GAction& action) {
        action.set_checked(!action.is_checked());
        player->hide_scope(action.is_checked());
    });
    hide_scope->set_checkable(true);

    app_menu->add_action(GCommonActions::make_open_action([&](auto&) {
        Optional<String> path = GFilePicker::get_open_filepath("Open wav file...");
        if (path.has_value()) {
            player->open_file(path.value());
        }
    }));
    app_menu->add_action(move(hide_scope));
    app_menu->add_separator();
    app_menu->add_action(GCommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [](auto&) {
        GAboutDialog::show("SoundPlayer", GraphicsBitmap::load_from_file("/res/icons/32x32/app-sound-player.png"));
    }));

    menubar->add_menu(move(app_menu));
    menubar->add_menu(move(help_menu));
    app.set_menubar(move(menubar));

    window->set_main_widget(player);
    window->show();

    return app.exec();
}
