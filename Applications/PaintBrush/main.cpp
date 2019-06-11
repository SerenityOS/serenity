#include "PaintableWidget.h"
#include "PaletteWidget.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("PaintBrush");
    window->set_rect(100, 100, 600, 434);

    auto* main_widget = new GWidget(nullptr);
    window->set_main_widget(main_widget);
    main_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    main_widget->layout()->set_spacing(0);

    auto* paintable_widget = new PaintableWidget(main_widget);
    auto* palette_widget = new PaletteWidget(*paintable_widget, main_widget);

    window->show();

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("PaintBrush");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [](const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    menubar->add_menu(move(file_menu));

    auto edit_menu = make<GMenu>("Edit");
    menubar->add_menu(move(edit_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [](const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
