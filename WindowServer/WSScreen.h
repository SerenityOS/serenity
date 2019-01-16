#pragma once

#include <Widgets/Rect.h>
#include <Widgets/Size.h>
#include <Kernel/Keyboard.h>

class WSScreen {
public:
    ~WSScreen();

    int width() const { return m_width; }
    int height() const { return m_height; }

    static WSScreen& the();

    Size size() const { return { width(), height() }; }
    Rect rect() const { return { 0, 0, width(), height() }; }

    static void initialize();

    Point cursor_location() const { return m_cursor_location; }
    bool left_mouse_button_pressed() const { return m_left_mouse_button_pressed; }
    bool right_mouse_button_pressed() const { return m_right_mouse_button_pressed; }

    void on_receive_mouse_data(int dx, int dy, bool left_button, bool right_button);
    void on_receive_keyboard_data(Keyboard::Key);

protected:
    WSScreen(unsigned width, unsigned height);

private:
    int m_width { 0 };
    int m_height { 0 };

    Point m_cursor_location;
    bool m_left_mouse_button_pressed { false };
    bool m_right_mouse_button_pressed { false };
};

