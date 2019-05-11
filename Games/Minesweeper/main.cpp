#include "Field.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GLabel.h>
#include <LibCore/CConfigFile.h>

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
    widget->layout()->set_spacing(0);

    auto* container = new GWidget(widget);
    container->set_fill_with_background_color(true);
    container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    container->set_preferred_size({ 0, 36 });
    container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    auto* flag_icon_label = new GLabel(container);
    flag_icon_label->set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/flag.png"));
    auto* flag_label = new GLabel(container);
    auto* face_button = new GButton(container);
    face_button->set_button_style(ButtonStyle::CoolBar);
    face_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    face_button->set_preferred_size({ 36, 0 });
    auto* time_icon_label = new GLabel(container);
    time_icon_label->set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/timer.png"));
    auto* time_label = new GLabel(container);
    auto* field = new Field(*flag_label, *time_label, *face_button, widget);

    field->on_size_changed = [&] {
        auto size = field->preferred_size();
        size.set_height(size.height() + container->preferred_size().height());
        window->resize(size);
    };

    {
        auto config = CConfigFile::get_for_app("Minesweeper");
        int mine_count = config->read_num_entry("Game", "MineCount", 10);
        int rows = config->read_num_entry("Game", "Rows", 9);
        int columns = config->read_num_entry("Game", "Columns", 9);
        field->set_field_size(rows, columns, mine_count);
    }

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
    game_menu->add_separator();
    game_menu->add_action(GAction::create("Beginner", { Mod_Ctrl, Key_B }, [field] (const GAction&) {
        field->set_field_size(9, 9, 10);
    }));
    game_menu->add_action(GAction::create("Intermediate", { Mod_Ctrl, Key_I }, [field] (const GAction&) {
        field->set_field_size(16, 16, 40);
    }));
    game_menu->add_action(GAction::create("Expert", { Mod_Ctrl, Key_E }, [field] (const GAction&) {
        field->set_field_size(16, 30, 99);
    }));
    game_menu->add_action(GAction::create("Madwoman", { Mod_Ctrl, Key_M }, [field] (const GAction&) {
        field->set_field_size(32, 60, 350);
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
