#pragma once

#include "Object.h"
#include "Rect.h"
#include "Size.h"
#include "Keyboard.h"
#include "PS2MouseDevice.h"

class AbstractScreen : public Object, public KeyboardClient {
public:
    virtual ~AbstractScreen();

    int width() const { return m_width; }
    int height() const { return m_height; }

    static AbstractScreen& the();

    Size size() const { return { width(), height() }; }
    Rect rect() const { return { 0, 0, width(), height() }; }

    static void initialize();

    Point cursor_location() const { return m_cursor_location; }
    bool left_mouse_button_pressed() const { return m_left_mouse_button_pressed; }
    bool right_mouse_button_pressed() const { return m_right_mouse_button_pressed; }

    void on_receive_mouse_data(int dx, int dy, bool left_button, bool right_button);

protected:
    AbstractScreen(unsigned width, unsigned height);

private:
    // ^KeyboardClient
    virtual void on_key_pressed(Keyboard::Key) final;

    int m_width { 0 };
    int m_height { 0 };

    Point m_cursor_location;
    bool m_left_mouse_button_pressed { false };
    bool m_right_mouse_button_pressed { false };
};

