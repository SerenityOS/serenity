#include "CalculatorWidget.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer rpath accept unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio shared_buffer rpath accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GWindow::construct();
    window->set_title("Calculator");
    window->set_resizable(false);
    window->set_rect({ 300, 200, 254, 213 });

    auto calc_widget = CalculatorWidget::construct(nullptr);
    window->set_main_widget(calc_widget);

    window->show();
    window->set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/app-calculator.png"));

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("Calculator");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Calculator", load_png("/res/icons/16x16/app-calculator.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
