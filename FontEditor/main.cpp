#include "FontEditor.h"
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    (void) argc;
    (void) argv;

    GEventLoop loop;
    auto* window = new GWindow;
    window->set_title("FontEditor");
    window->set_rect({ 50, 50, 420, 200 });
    auto* font_editor = new FontEditorWidget;
    font_editor->set_relative_rect({ 0, 0, 420, 200 });
    window->set_main_widget(font_editor);
    window->show();
    return loop.exec();
}
