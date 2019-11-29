#include "PaintableWidget.h"
#include "PaletteWidget.h"
#include "ToolboxWidget.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_title("PaintBrush");
    window->set_rect(100, 100, 640, 480);
    window->set_icon(load_png("/res/icons/16x16/app-paintbrush.png"));

    auto horizontal_container = GWidget::construct();
    window->set_main_widget(horizontal_container);
    horizontal_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    horizontal_container->layout()->set_spacing(0);

    new ToolboxWidget(horizontal_container);

    auto vertical_container = GWidget::construct(horizontal_container.ptr());
    vertical_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    vertical_container->layout()->set_spacing(0);

    auto paintable_widget = PaintableWidget::construct(vertical_container);
    paintable_widget->set_focus(true);
    PaletteWidget::construct(*paintable_widget, vertical_container);

    window->show();

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("PaintBrush");

    app_menu->add_action(GCommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GFilePicker::get_open_filepath();

        if (!open_path.has_value())
            return;

        auto bitmap = load_png(open_path.value());
        if (!bitmap) {
            GMessageBox::show(String::format("Failed to load '%s'", open_path.value().characters()), "Open failed", GMessageBox::Type::Error, GMessageBox::InputType::OK, window);
            return;
        }
        paintable_widget->set_bitmap(*bitmap);
    }));
    app_menu->add_separator();
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));

    menubar->add_menu(move(app_menu));

    auto edit_menu = make<GMenu>("Edit");
    menubar->add_menu(move(edit_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [&](auto&) {
        GAboutDialog::show("PaintBrush", load_png("/res/icons/32x32/app-paintbrush.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
