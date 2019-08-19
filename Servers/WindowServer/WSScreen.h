#pragma once

#include <Kernel/KeyCode.h>
#include <LibDraw/Color.h>
#include <LibDraw/Rect.h>
#include <LibDraw/Size.h>

class WSScreen {
public:
    WSScreen(unsigned width, unsigned height);
    ~WSScreen();

    void set_resolution(int width, int height);
    bool can_set_buffer() { return m_can_set_buffer; }
    void set_buffer(int index);

    int width() const { return m_width; }
    int height() const { return m_height; }
    size_t pitch() const { return m_pitch; }
    RGBA32* scanline(int y);

    static WSScreen& the();

    Size size() const { return { width(), height() }; }
    Rect rect() const { return { 0, 0, width(), height() }; }

    Point cursor_location() const { return m_cursor_location; }
    unsigned mouse_button_state() const { return m_mouse_button_state; }

    void on_receive_mouse_data(int dx, int dy, int dz, unsigned buttons);
    void on_receive_keyboard_data(KeyEvent);

private:
    void on_change_resolution(int pitch, int width, int height);

    size_t m_size_in_bytes;

    RGBA32* m_framebuffer { nullptr };
    bool m_can_set_buffer { false };

    int m_pitch { 0 };
    int m_width { 0 };
    int m_height { 0 };
    int m_framebuffer_fd { -1 };

    Point m_cursor_location;
    unsigned m_mouse_button_state { 0 };
    unsigned m_modifiers { 0 };
};

inline RGBA32* WSScreen::scanline(int y)
{
    return reinterpret_cast<RGBA32*>(((u8*)m_framebuffer) + (y * m_pitch));
}
