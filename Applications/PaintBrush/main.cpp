#include "PaintableWidget.h"
#include "PaletteWidget.h"
#include "ToolboxWidget.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GWindow.h>
#include <LibDraw/PNGLoader.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("PaintBrush");
    window->set_rect(100, 100, 640, 480);

    auto* horizontal_container = new GWidget(nullptr);
    window->set_main_widget(horizontal_container);
    horizontal_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    horizontal_container->layout()->set_spacing(0);

    new ToolboxWidget(horizontal_container);

    auto* vertical_container = new GWidget(horizontal_container);
    vertical_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    vertical_container->layout()->set_spacing(0);

    auto* paintable_widget = new PaintableWidget(vertical_container);
    new PaletteWidget(*paintable_widget, vertical_container);

    window->show();

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("PaintBrush");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [](const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    file_menu->add_action(GAction::create("Open...", { Mod_Ctrl, Key_O }, [&](auto&) {
        GFilePicker picker;
        if (picker.exec() == GFilePicker::ExecOK) {
            auto filename = picker.selected_file().string();
            auto bitmap = load_png(filename);
            if (!bitmap) {
                GMessageBox msgbox(String::format("Failed to load '%s'", filename.characters()), "Open failed", GMessageBox::Type::Error, GMessageBox::InputType::OK, window);
                msgbox.exec();
                return;
            }
            paintable_widget->set_bitmap(*bitmap);
        }
    }));
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
