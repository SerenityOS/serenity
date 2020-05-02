#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

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

    window->show();
    return app.exec();
}
