#include "FontEditor.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    RefPtr<Font> edited_font;
    String path;

    if (argc == 2) {
        path = argv[1];
        edited_font = Font::load_from_file(path);
        if (!edited_font) {
            fprintf(stderr, "Couldn't load font: %s\n", path.characters());
            return 1;
        }
    }

    if (edited_font)
        edited_font = edited_font->clone();
    else
        edited_font = Font::default_font().clone();

    auto window = GWindow::construct();
    window->set_title("Font Editor");
    window->set_rect({ 50, 50, 390, 342 });

    auto font_editor = FontEditorWidget::construct(path, move(edited_font));
    window->set_main_widget(font_editor);
    window->show();
    window->set_icon(load_png("/res/icons/16x16/app-font-editor.png"));

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("Font Editor");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Font Editor", load_png("/res/icons/FontEditor.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
