#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <SharedGraphics/PNGLoader.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("QuickShow");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [](const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    menubar->add_menu(move(file_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [](const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

#if 0
    if (argc != 2) {
        printf("usage: qs <image-file>\n");
        return 0;
    }
#endif

    const char* path = "/res/wallpapers/sunset-retro.png";
    if (argc > 1)
        path = argv[1];

    auto bitmap = load_png(path);
    if (!bitmap) {
        fprintf(stderr, "Failed to load %s\n", path);
        return 1;
    }

    auto* window = new GWindow;

    window->set_double_buffering_enabled(false);
    window->set_title(String::format("QuickShow: %s %s", path, bitmap->size().to_string().characters()));
    window->set_rect(200, 200, bitmap->width(), bitmap->height());

    auto* widget = new GWidget;
    window->set_main_widget(widget);

    if (bitmap->has_alpha_channel()) {
        widget->set_background_color(Color::White);
        widget->set_fill_with_background_color(true);
    }

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* label = new GLabel(widget);
    label->set_icon(move(bitmap));
    label->set_should_stretch_icon(true);

    window->set_should_exit_event_loop_on_close(true);
    window->show();

    return app.exec();
}
