#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_rect(100, 100, 240, 160);
    window->set_title("Hello World!");

    auto main_widget = GWidget::construct();
    window->set_main_widget(main_widget);
    main_widget->set_fill_with_background_color(true);
    main_widget->set_background_color(Color::White);
    main_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    main_widget->layout()->set_margins({ 4, 4, 4, 4 });

    auto label = GLabel::construct(main_widget);
    label->set_text("Hello\nWorld!");

    auto button = GButton::construct(main_widget);
    button->set_text("Good-bye");
    button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button->set_preferred_size(0, 20);
    button->on_click = [&](GButton&) {
        app.quit();
    };

    window->show();

    return app.exec();
}
