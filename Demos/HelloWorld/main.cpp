#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_rect(100, 100, 240, 160);
    window->set_title("Hello World!");

    auto* main_widget = new GWidget;
    window->set_main_widget(main_widget);
    main_widget->set_fill_with_background_color(true);
    main_widget->set_background_color(Color::White);
    main_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    main_widget->layout()->set_margins({ 4, 4, 4, 4 });

    auto* label = new GLabel(main_widget);
    label->set_text("Hello\nWorld!");

    auto* button = new GButton(main_widget);
    button->set_text("Good-bye");
    button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button->set_preferred_size(0, 20);
    button->on_click = [&](GButton&) {
        app.quit();
    };

    window->show();

    return app.exec();
}
