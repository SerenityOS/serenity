#include "WSScreen.h"
#include "WSEventLoop.h"
#include "WSEvent.h"
#include "WSWindowManager.h"
#include <AK/Assertions.h>

static WSScreen* s_the;

void WSScreen::initialize()
{
    s_the = nullptr;
}

WSScreen& WSScreen::the()
{
    ASSERT(s_the);
    return *s_the;
}

WSScreen::WSScreen(unsigned width, unsigned height)
    : m_width(width)
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
    if (m_cursor_location != prev_location) {
        auto event = make<MouseEvent>(WSEvent::MouseMove, m_cursor_location.x(), m_cursor_location.y());
        WSEventLoop::the().post_event(&WSWindowManager::the(), move(event));
    }
    bool prev_left_button = m_left_mouse_button_pressed;
    bool prev_right_button = m_right_mouse_button_pressed;
    m_left_mouse_button_pressed = left_button;
    m_right_mouse_button_pressed = right_button;
    if (prev_left_button != left_button) {
        auto event = make<MouseEvent>(left_button ? WSEvent::MouseDown : WSEvent::MouseUp, m_cursor_location.x(), m_cursor_location.y(), MouseButton::Left);
        WSEventLoop::the().post_event(&WSWindowManager::the(), move(event));
    }
    if (prev_right_button != right_button) {
        auto event = make<MouseEvent>(right_button ? WSEvent::MouseDown : WSEvent::MouseUp, m_cursor_location.x(), m_cursor_location.y(), MouseButton::Right);
        WSEventLoop::the().post_event(&WSWindowManager::the(), move(event));
    }
    if (m_cursor_location != prev_location || prev_left_button != left_button)
        WSWindowManager::the().draw_cursor();
}

void WSScreen::on_receive_keyboard_data(Keyboard::Key key)
{
    auto event = make<KeyEvent>(WSEvent::KeyDown, 0);
    int key_code = 0;

    switch (key.character) {
    case 8: key_code = KeyboardKey::Backspace; break;
    case 10: key_code = KeyboardKey::Return; break;
    }
    event->m_key = key_code;

    if (key.character) {
        char buf[] = { 0, 0 };
        char& ch = buf[0];
        ch = key.character;
        if (key.shift()) {
            if (ch >= 'a' && ch <= 'z') {
                ch &= ~0x20;
            } else {
                switch (ch) {
                case '1': ch = '!'; break;
                case '2': ch = '@'; break;
                case '3': ch = '#'; break;
                case '4': ch = '$'; break;
                case '5': ch = '%'; break;
                case '6': ch = '^'; break;
                case '7': ch = '&'; break;
                case '8': ch = '*'; break;
                case '9': ch = '('; break;
                case '0': ch = ')'; break;
                case '-': ch = '_'; break;
                case '=': ch = '+'; break;
                case '`': ch = '~'; break;
                case ',': ch = '<'; break;
                case '.': ch = '>'; break;
                case '/': ch = '?'; break;
                case '[': ch = '{'; break;
                case ']': ch = '}'; break;
                case '\\': ch = '|'; break;
                case '\'': ch = '"'; break;
                case ';': ch = ':'; break;
                }
            }
        }
        event->m_text = buf;
    }

    event->m_shift = key.shift();
    event->m_ctrl = key.ctrl();
    event->m_alt = key.alt();

    WSEventLoop::the().post_event(&WSWindowManager::the(), move(event));
}
