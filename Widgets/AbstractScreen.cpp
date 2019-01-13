#include "AbstractScreen.h"
#include "EventLoop.h"
#include "Event.h"
#include "Widget.h"
#include "WindowManager.h"
#include <AK/Assertions.h>

static AbstractScreen* s_the;

void AbstractScreen::initialize()
{
    s_the = nullptr;
}

AbstractScreen& AbstractScreen::the()
{
    ASSERT(s_the);
    return *s_the;
}

AbstractScreen::AbstractScreen(unsigned width, unsigned height)
    : Object(nullptr)
    , m_width(width)
    , m_height(height)
{
    ASSERT(!s_the);
    s_the = this;

    m_cursor_location = rect().center();

    Keyboard::the().set_client(this);
}

AbstractScreen::~AbstractScreen()
{
}

void AbstractScreen::on_receive_mouse_data(int dx, int dy, bool left_button, bool right_button)
{
    auto prev_location = m_cursor_location;
    m_cursor_location.moveBy(dx, dy);
    m_cursor_location.constrain(rect());
    if (m_cursor_location.x() >= width())
        m_cursor_location.setX(width() - 1);
    if (m_cursor_location.y() >= height())
        m_cursor_location.setY(height() - 1);
    if (m_cursor_location != prev_location) {
        auto event = make<MouseEvent>(Event::MouseMove, m_cursor_location.x(), m_cursor_location.y());
        EventLoop::main().postEvent(&WindowManager::the(), move(event));
    }
    bool prev_left_button = m_left_mouse_button_pressed;
    bool prev_right_button = m_right_mouse_button_pressed;
    m_left_mouse_button_pressed = left_button;
    m_right_mouse_button_pressed = right_button;
    if (prev_left_button != left_button) {
        auto event = make<MouseEvent>(left_button ? Event::MouseDown : Event::MouseUp, m_cursor_location.x(), m_cursor_location.y(), MouseButton::Left);
        EventLoop::main().postEvent(&WindowManager::the(), move(event));
    }
    if (prev_right_button != right_button) {
        auto event = make<MouseEvent>(right_button ? Event::MouseDown : Event::MouseUp, m_cursor_location.x(), m_cursor_location.y(), MouseButton::Right);
        EventLoop::main().postEvent(&WindowManager::the(), move(event));
    }
    if (m_cursor_location != prev_location || prev_left_button != left_button)
        WindowManager::the().redraw_cursor();
}

void AbstractScreen::on_key_pressed(Keyboard::Key key)
{
    auto event = make<KeyEvent>(Event::KeyDown, 0);
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

    EventLoop::main().postEvent(&WindowManager::the(), move(event));
}
