#pragma once

#include <SharedGraphics/Rect.h>
#include <SharedGraphics/Size.h>
#include <SharedGraphics/Color.h>
#include <Kernel/KeyCode.h>

class WSScreen {
public:
    WSScreen(unsigned width, unsigned height);
    ~WSScreen();

    void set_resolution(int width, int height);

    int width() const { return m_width; }
    int height() const { return m_height; }
    RGBA32* scanline(int y);

    static WSScreen& the();

    Size size() const { return { width(), height() }; }
    Rect rect() const { return { 0, 0, width(), height() }; }

    void set_y_offset(int);

    Point cursor_location() const { return m_cursor_location; }
    bool left_mouse_button_pressed() const { return m_left_mouse_button_pressed; }
    bool right_mouse_button_pressed() const { return m_right_mouse_button_pressed; }
    bool middle_mouse_button_pressed() const { return m_middle_mouse_button_pressed; }

    void on_receive_mouse_data(int dx, int dy, bool left_button, bool right_button, bool middle_button);
    void on_receive_keyboard_data(KeyEvent);

private:
    RGBA32* m_framebuffer { nullptr };

    int m_width { 0 };
    int m_height { 0 };
    int m_framebuffer_fd { -1 };

    Point m_cursor_location;
    bool m_left_mouse_button_pressed { false };
    bool m_right_mouse_button_pressed { false };
    bool m_middle_mouse_button_pressed { false };
};

inline RGBA32* WSScreen::scanline(int y)
{
    size_t pitch = sizeof(RGBA32) * width();
    return reinterpret_cast<RGBA32*>(((byte*)m_framebuffer) + (y * pitch));
}
