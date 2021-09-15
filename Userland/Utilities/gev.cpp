/*
 * Copyright (c) 2021, xSlendiX <gamingxslendix@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <unistd.h>

class EventViewerWidget : public GUI::Widget {
    C_OBJECT(EventViewerWidget);

public:
    void set_mouse_enabled(bool mouse_enabled) { m_mouse_enabled = mouse_enabled; }
    void set_key_enabled(bool key_enabled) { m_key_enabled = key_enabled; }
    void set_drag_enabled(bool drag_enabled) { m_drag_enabled = drag_enabled; }
    void set_drop_enabled(bool drop_enabled) { m_drop_enabled = drop_enabled; }
    void set_resize_enabled(bool resize_enabled) { m_resize_enabled = resize_enabled; }
    void set_enter_enabled(bool enter_enabled) { m_enter_enabled = enter_enabled; }
    void set_leave_enabled(bool leave_enabled) { m_leave_enabled = leave_enabled; }
    void set_theme_enabled(bool theme_enabled) { m_theme_enabled = theme_enabled; }
    void set_fonts_enabled(bool fonts_enabled) { m_fonts_enabled = fonts_enabled; }

protected:
    virtual ~EventViewerWidget() override
    {
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());

        painter.fill_rect(rect(), Color::White);
    }

    virtual void resize_event(GUI::ResizeEvent& event) override
    {
        if (!m_resize_enabled)
            return;
        outln("Resize: ");
        outln("    Size: {}", event.size());
        outln();
    }

    virtual void keydown_event(GUI::KeyEvent& event) override
    {
        print_key_event(event, true);
    }

    virtual void keyup_event(GUI::KeyEvent& event) override
    {
        print_key_event(event, false);
    }

    virtual void mousemove_event(GUI::MouseEvent& event) override
    {
        print_mouse_event(event, "MouseMove");
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        print_mouse_event(event, "MouseDown");
    }

    virtual void mouseup_event(GUI::MouseEvent& event) override
    {
        print_mouse_event(event, "MouseUp:");
    }

    virtual void mousewheel_event(GUI::MouseEvent& event) override
    {
        print_mouse_event(event, "MouseWheel:");
    }

    virtual void doubleclick_event(GUI::MouseEvent& event) override
    {
        print_mouse_event(event, "DoubleClick:");
    }

    virtual void context_menu_event(GUI::ContextMenuEvent& event) override
    {
        outln("ContextMenu:");
        outln(
            "    Position: {}, ScreenPosition: {}",
            event.position(), event.screen_position());
        outln();
    }

    virtual void enter_event(Core::Event&) override
    {
        if (!m_enter_enabled)
            return;
        outln("Enter:");
        outln();
    }

    virtual void leave_event(Core::Event&) override
    {
        if (!m_leave_enabled)
            return;
        outln("Leave:");
        outln();
    }

    virtual void drag_enter_event(GUI::DragEvent& event) override
    {
        print_drag_event(event, "DragEnter:");
    }

    virtual void drag_move_event(GUI::DragEvent& event) override
    {
        print_drag_event(event, "DragMove:");
    }

    virtual void drag_leave_event(GUI::Event&) override
    {
        if (!m_drag_enabled)
            return;
        outln("DragLeave:");
        outln();
    }

    virtual void drop_event(GUI::DropEvent& event) override
    {
        event.accept();
        print_drop_event(event);
    }

    virtual void theme_change_event(GUI::ThemeChangeEvent&) override
    {
        if (!m_theme_enabled)
            return;
        outln("ThemeChange:");
        outln();
    }

    virtual void fonts_change_event(GUI::FontsChangeEvent&) override
    {
        if (!m_fonts_enabled)
            return;
        outln("FontsChange:");
        outln();
    }

private:
    void print_modifiers(bool ctrl, bool alt, bool shift, bool super, bool altgr)
    {
        Vector<String> string_vector;

        if (ctrl)
            string_vector.append("Mod_Ctrl");

        if (alt)
            string_vector.append("Mod_Alt");

        if (shift)
            string_vector.append("Mod_Shift");

        if (super)
            string_vector.append("Mod_Super");

        if (altgr)
            string_vector.append("Mod_AltGr");

        if (string_vector.size()) {
            out("    Modifiers: ");
            for (size_t i = 0; i < string_vector.size(); ++i) {
                if (i)
                    out(" | ");
                out(string_vector[i]);
            }
        }
    }

    void print_key_event(GUI::KeyEvent& event, bool keydown)
    {
        if (!m_key_enabled)
            return;
        outln("Key: ");
        if (keydown) {
            outln("    Type: KeyDown");
        } else {
            outln("    Type: KeyUp");
        }
        if (event.text().is_empty()) {
            outln(
                "    KeyCode: {} (0x{:hex-dump})",
                (u8)event.key(), (u8)event.key());
        } else {
            outln(
                "    Key: {}, KeyCode: {} (0x{:hex-dump})",
                event.text(), (u8)event.key(), (u8)event.key());
        }

        print_modifiers(event.ctrl(), event.alt(), event.shift(), event.super(), event.modifiers() & Mod_AltGr);
        outln();
    }

    void print_mouse_event(GUI::MouseEvent& event, String event_type)
    {
        if (!m_mouse_enabled)
            return;
        outln("{}:", event_type);
        outln("    Position: {}", event.position());
        String button = "Unknown";
        switch (event.button()) {
        case GUI::MouseButton::None:
            button = "None";
            break;
        case GUI::MouseButton::Left:
            button = "Left";
            break;
        case GUI::MouseButton::Middle:
            button = "Middle";
            break;
        case GUI::MouseButton::Right:
            button = "Right";
            break;
        case GUI::MouseButton::Forward:
            button = "Forward";
            break;
        case GUI::MouseButton::Back:
            button = "Back";
            break;
        default:
            button = "Unknown";
        }
        outln("    Button: {}, Buttons: {}", button, event.buttons());
        print_modifiers(event.ctrl(), event.alt(), event.shift(), event.super(), event.modifiers() & Mod_AltGr);
        outln("    WheelDelta: {}", event.wheel_delta());
        outln();
    }

    void print_drag_event(GUI::DragEvent& event, String event_type)
    {
        if (!m_drag_enabled)
            return;
        outln("{}:", event_type);
        outln("    Position: {}", event.position());
        outln("    MIMETypes: {}", event.mime_types());
        outln();
    }

    void print_drop_event(GUI::DropEvent& event)
    {
        if (!m_drop_enabled)
            return;
        outln("Drop:");
        outln("    Position: {}", event.position());
        outln("    Text: {}", event.text());
        StringBuilder mime_data;
        mime_data.append(String::formatted("    MIMETypes: {}\n", event.mime_data().formats()));
        if (event.mime_data().has_text()) {
            auto data = event.mime_data().text();
            if (data.length() > 10) {
                data = data.substring(10);
            }
            mime_data.append(String::formatted("    MIMEText: {}\n", data));
        }
        if (event.mime_data().has_urls()) {
            mime_data.append(String::formatted("    MIMEURLs: {}", event.mime_data().urls()));
        }
        outln(mime_data.to_string());
        outln();
    }

    bool m_mouse_enabled = false;
    bool m_key_enabled = false;
    bool m_drag_enabled = false;
    bool m_drop_enabled = false;
    bool m_resize_enabled = false;
    bool m_enter_enabled = false;
    bool m_leave_enabled = false;
    bool m_theme_enabled = false;
    bool m_fonts_enabled = false;
};

int main(int argc, char** argv)
{
    if (pledge("stdio rpath unix recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    bool mouse_enabled = false;
    bool key_enabled = false;
    bool drag_enabled = false;
    bool drop_enabled = false;
    bool resize_enabled = false;
    bool enter_enabled = false;
    bool leave_enabled = false;
    bool theme_enabled = false;
    bool fonts_enabled = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("GUI event viewing tool");
    args_parser.add_option(mouse_enabled, "Show mouse-only output", "mouse", 'm');
    args_parser.add_option(key_enabled, "Show key-only output", "key", 'k');
    args_parser.add_option(drag_enabled, "Show drag-only output", "drag", 'd');
    args_parser.add_option(drop_enabled, "Show drop-only output", "drop", 'D');
    args_parser.add_option(resize_enabled, "Show resize-only output", "resize", 'r');
    args_parser.add_option(enter_enabled, "Show enter-only output", "enter", 'e');
    args_parser.add_option(leave_enabled, "Show leave-only output", "leave", 'l');
    args_parser.add_option(theme_enabled, "Show theme-only output", "theme", 't');
    args_parser.add_option(fonts_enabled, "Show fonts-only output", "fonts", 'f');
    args_parser.parse(argc, argv);

    if (
        !(mouse_enabled || key_enabled || drag_enabled || drop_enabled || resize_enabled || enter_enabled || leave_enabled || theme_enabled || fonts_enabled)) {
        mouse_enabled = key_enabled = drag_enabled = drop_enabled = resize_enabled = enter_enabled = leave_enabled = theme_enabled = fonts_enabled = true;
    }

    auto window = GUI::Window::construct();
    window->set_title("GUI Event viewer");

    auto& main_widget = window->set_main_widget<EventViewerWidget>();

    main_widget.set_mouse_enabled(mouse_enabled);
    main_widget.set_key_enabled(key_enabled);
    main_widget.set_drag_enabled(drag_enabled);
    main_widget.set_drop_enabled(drop_enabled);
    main_widget.set_resize_enabled(resize_enabled);
    main_widget.set_enter_enabled(enter_enabled);
    main_widget.set_leave_enabled(leave_enabled);
    main_widget.set_theme_enabled(theme_enabled);
    main_widget.set_fonts_enabled(fonts_enabled);

    window->show();
    return app->exec();
}
