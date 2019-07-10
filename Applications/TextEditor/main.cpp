#include "TextEditor.h"

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("Text Editor");
    window->set_rect(20, 200, 640, 400);
    auto* text_widget = new TextEditorWidget(&app, window, argc, argv);
    window->set_main_widget(text_widget);
    window->set_should_exit_event_loop_on_close(true);
    window->show();
    window->set_icon_path("/res/icons/TextEditor16.png");

    return app.exec();
}