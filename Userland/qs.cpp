#include <SharedGraphics/PNGLoader.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GLabel.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

#if 0
    if (argc != 2) {
        printf("usage: qs <image-file>\n");
        return 0;
    }
#endif

    const char* path = "/res/icons/folder32.png";
    if (argc > 1)
        path = argv[1];

    auto bitmap = load_png(path);
    if (!bitmap) {
        fprintf(stderr, "Failed to load %s\n", path);
        return 1;
    }

    auto* window = new GWindow;
    window->set_title(String::format("qs: %s", path));
    window->set_rect(200, 200, bitmap->width(), bitmap->height());

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    window->set_main_widget(widget);

    auto* label = new GLabel(widget);
    label->set_relative_rect({ 0, 0, bitmap->width(), bitmap->height() });
    label->set_icon(move(bitmap));

    window->set_should_exit_event_loop_on_close(true);
    window->show();

    return app.exec();
}
