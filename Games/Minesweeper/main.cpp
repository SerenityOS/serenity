#include "Field.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GLabel.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_should_exit_event_loop_on_close(true);
    window->set_resizable(false);
    window->set_title("Minesweeper");
    window->set_rect(100, 100, 139, 175);

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* container = new GWidget(widget);
    container->set_fill_with_background_color(true);
    container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    container->set_preferred_size({ 0, 36 });
    container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    auto* flag_icon_label = new GLabel(container);
    flag_icon_label->set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/flag.png"));
    auto* flag_label = new GLabel(container);
    auto* face_button = new GButton(container);
    auto* time_icon_label = new GLabel(container);
    time_icon_label->set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/timer.png"));
    auto* time_label = new GLabel(container);
    auto* field = new Field(*flag_label, *time_label, *face_button, widget);

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("Minesweeper");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [] (const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto game_menu = make<GMenu>("Game");
    game_menu->add_action(GAction::create("New game", { Mod_None, Key_F2 }, [field] (const GAction&) {
        field->reset();
    }));
    menubar->add_menu(move(game_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->show();

    window->set_icon_path("/res/icons/minesweeper/mine.png");

    return app.exec();
}
