#include "FontEditor.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    RetainPtr<Font> edited_font;
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

    auto* window = new GWindow;
    window->set_title("FontEditor");
    window->set_rect({ 50, 50, 420, 300 });
    auto* font_editor = new FontEditorWidget(path, move(edited_font));
    window->set_main_widget(font_editor);
    window->set_should_exit_event_loop_on_close(true);
    window->show();
    return app.exec();
}
