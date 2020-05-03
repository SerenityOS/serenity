#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

static unsigned s_mouse_button_state;

class MouseButtonIndicator final : public GUI::Frame {
    C_OBJECT(MouseButtonIndicator);

public:
    virtual ~MouseButtonIndicator() {}

private:
    virtual void paint_event(GUI::PaintEvent& event) override
    {
        Frame::paint_event(event);

        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());

        Color background_color;
        Color foreground_color;

        if (s_mouse_button_state & m_button) {
            background_color = Color::Black;
            foreground_color = Color::White;
        } else {
            background_color = Color::White;
            foreground_color = Color::Black;
        }

        painter.fill_rect(frame_inner_rect(), background_color);
        painter.draw_text(frame_inner_rect(), m_name, Gfx::TextAlignment::Center, foreground_color);
    }

    void mousedown_event(GUI::MouseEvent& event) override
    {
        event.ignore();
    }

    void mouseup_event(GUI::MouseEvent& event) override
    {
        event.ignore();
    }

    MouseButtonIndicator(const String& name, GUI::MouseButton button)
        : m_name(name)
        , m_button(button)
    {
    }

    String m_name;
    GUI::MouseButton m_button;
};

class MainWidget final : public GUI::Widget {
    C_OBJECT(MainWidget);

public:
    void mousedown_event(GUI::MouseEvent& event) override
    {
        s_mouse_button_state = event.buttons();
        update();
        Widget::mousedown_event(event);
    }

    void mouseup_event(GUI::MouseEvent& event) override
    {
        s_mouse_button_state = event.buttons();
        update();
        Widget::mouseup_event(event);
    }

private:
    MainWidget() {}
};

int main(int argc, char** argv)
{
    GUI::Application app(argc, argv);
    auto window = GUI::Window::construct();
    window->set_title("Mouse button demo");

    auto& main_widget = window->set_main_widget<MainWidget>();
    main_widget.set_fill_with_background_color(true);

    main_widget.set_layout<GUI::VerticalBoxLayout>();

    main_widget.add<MouseButtonIndicator>("Left", GUI::MouseButton::Left);
    main_widget.add<MouseButtonIndicator>("Middle", GUI::MouseButton::Middle);
    main_widget.add<MouseButtonIndicator>("Right", GUI::MouseButton::Right);
    main_widget.add<MouseButtonIndicator>("Back", GUI::MouseButton::Back);
    main_widget.add<MouseButtonIndicator>("Forward", GUI::MouseButton::Forward);

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("Mouse Demo");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app.quit(); }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("Mouse Demo", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-mouse.png"), window);
    }));

    app.set_menubar(move(menubar));
    window->show();
    return app.exec();
}
