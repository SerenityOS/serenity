#include "WSScreen.h"
#include "WSMessageLoop.h"
#include "WSMessage.h"
#include "WSWindowManager.h"
#include <AK/Assertions.h>
#include <Kernel/IO.h>

static WSScreen* s_the;

WSScreen& WSScreen::the()
{
    ASSERT(s_the);
    return *s_the;
}

WSScreen::WSScreen(RGBA32* framebuffer, unsigned width, unsigned height)
    : m_framebuffer(framebuffer)
    , m_width(width)
    , m_height(height)
{
    ASSERT(!s_the);
    s_the = this;

    m_cursor_location = rect().center();
}

WSScreen::~WSScreen()
{
}

void WSScreen::on_receive_mouse_data(int dx, int dy, bool left_button, bool right_button)
{
    auto prev_location = m_cursor_location;
    m_cursor_location.move_by(dx, dy);
    m_cursor_location.constrain(rect());
    if (m_cursor_location.x() >= width())
        m_cursor_location.set_x(width() - 1);
    if (m_cursor_location.y() >= height())
        m_cursor_location.set_y(height() - 1);
    unsigned buttons = 0;
    if (left_button)
        buttons |= (unsigned)MouseButton::Left;
    if (right_button)
        buttons |= (unsigned)MouseButton::Right;
    if (m_cursor_location != prev_location) {
        auto message = make<WSMouseEvent>(WSMessage::MouseMove, m_cursor_location, buttons);
        WSMessageLoop::the().post_message(&WSWindowManager::the(), move(message));
    }
    bool prev_left_button = m_left_mouse_button_pressed;
    bool prev_right_button = m_right_mouse_button_pressed;
    m_left_mouse_button_pressed = left_button;
    m_right_mouse_button_pressed = right_button;
    if (prev_left_button != left_button) {
        auto message = make<WSMouseEvent>(left_button ? WSMessage::MouseDown : WSMessage::MouseUp, m_cursor_location, buttons, MouseButton::Left);
        WSMessageLoop::the().post_message(&WSWindowManager::the(), move(message));
    }
    if (prev_right_button != right_button) {
        auto message = make<WSMouseEvent>(right_button ? WSMessage::MouseDown : WSMessage::MouseUp, m_cursor_location, buttons, MouseButton::Right);
        WSMessageLoop::the().post_message(&WSWindowManager::the(), move(message));
    }
    if (m_cursor_location != prev_location || prev_left_button != left_button)
        WSWindowManager::the().invalidate_cursor();
}

void WSScreen::on_receive_keyboard_data(Keyboard::Event kernel_event)
{
    auto message = make<WSKeyEvent>(kernel_event.is_press() ? WSMessage::KeyDown : WSMessage::KeyUp, kernel_event.key, kernel_event.character);
    message->m_shift = kernel_event.shift();
    message->m_ctrl = kernel_event.ctrl();
    message->m_alt = kernel_event.alt();
    WSMessageLoop::the().post_message(&WSWindowManager::the(), move(message));
}
