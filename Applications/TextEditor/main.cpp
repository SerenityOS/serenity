#include "TextEditorWidget.h"

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("Text Editor");

    window->set_rect(20, 200, 640, 400);
    window->set_should_exit_event_loop_on_close(true);
    window->set_icon_path("/res/icons/TextEditor16.png");

    auto* text_widget = new TextEditorWidget();
    window->set_main_widget(text_widget);

    if (argc >= 2)
        text_widget->open_sesame(argv[1]);

    window->show();

    return app.exec();
}