#include "Field.h"
#include <LibCore/CConfigFile.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_resizable(false);
    window->set_title("Minesweeper");
    window->set_rect(100, 100, 139, 175);

    auto widget = GWidget::construct();
    window->set_main_widget(widget);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    auto container = GWidget::construct(widget.ptr());
    container->set_fill_with_background_color(true);
    container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    container->set_preferred_size(0, 36);
    container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    auto flag_icon_label = GLabel::construct(container);
    flag_icon_label->set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/flag.png"));
    auto flag_label = GLabel::construct(container);
    auto face_button = GButton::construct(container);
    face_button->set_button_style(ButtonStyle::CoolBar);
    face_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    face_button->set_preferred_size(36, 0);
    auto time_icon_label = GLabel::construct(container);
    time_icon_label->set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/timer.png"));
    auto time_label = GLabel::construct(container);
    auto field = Field::construct(*flag_label, *time_label, *face_button, widget, [&](Size size) {
        size.set_height(size.height() + container->preferred_size().height());
        window->resize(size);
    });

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("Minesweeper");

    app_menu->add_action(GAction::create("New game", { Mod_None, Key_F2 }, [&](const GAction&) {
        field->reset();
    }));


    app_menu->add_separator();


    NonnullRefPtr<GAction> chord_toggler_action = GAction::create("Single-click chording", [&](const GAction&) {
        bool toggled = !field->is_single_chording();
        field->set_single_chording(toggled);
        chord_toggler_action->set_checked(toggled);
    });
    chord_toggler_action->set_checkable(true);
    chord_toggler_action->set_checked(field->is_single_chording());

    app_menu->add_action(*chord_toggler_action);
    app_menu->add_separator();

    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto difficulty_menu = GMenu::construct("Difficulty");
    difficulty_menu->add_action(GAction::create("Beginner", { Mod_Ctrl, Key_B }, [&](const GAction&) {
        field->set_field_size(9, 9, 10);
    }));
    difficulty_menu->add_action(GAction::create("Intermediate", { Mod_Ctrl, Key_I }, [&](const GAction&) {
        field->set_field_size(16, 16, 40);
    }));
    difficulty_menu->add_action(GAction::create("Expert", { Mod_Ctrl, Key_E }, [&](const GAction&) {
        field->set_field_size(16, 30, 99);
    }));
    difficulty_menu->add_action(GAction::create("Madwoman", { Mod_Ctrl, Key_M }, [&](const GAction&) {
        field->set_field_size(32, 60, 350);
    }));
    menubar->add_menu(move(difficulty_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Minesweeper", load_png("/res/icons/32x32/app-minesweeper.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->show();

    window->set_icon(load_png("/res/icons/minesweeper/mine.png"));

    return app.exec();
}
