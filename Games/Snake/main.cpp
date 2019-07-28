#include "SnakeGame.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;

    window->set_double_buffering_enabled(false);
    window->set_title("Snake");
    window->set_rect(100, 100, 320, 320);

    auto* game = new SnakeGame;
    window->set_main_widget(game);

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("Snake");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [](const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto game_menu = make<GMenu>("Game");
    game_menu->add_action(GAction::create("New game", { Mod_None, Key_F2 }, [&](const GAction&) {
        game->reset();
    }));
    menubar->add_menu(move(game_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [](const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->show();

    window->set_icon(load_png("/res/icons/16x16/app-snake.png"));

    return app.exec();
}
