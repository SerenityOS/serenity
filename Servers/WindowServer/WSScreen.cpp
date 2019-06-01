#include "WSCompositor.h"
#include "WSEvent.h"
#include "WSEventLoop.h"
#include "WSScreen.h"
#include "WSWindowManager.h"
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <unistd.h>
#include <AK/StringBuilder.h>

static WSScreen* s_the;

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
    m_framebuffer_fd = open("/dev/bxvga", O_RDWR);
    ASSERT(m_framebuffer_fd >= 0);

    set_resolution(width, height);
}

WSScreen::~WSScreen()
{
}

void WSScreen::set_resolution(int width, int height)
{
    struct BXVGAResolution {
        int width;
        int height;
    };
    BXVGAResolution resolution { (int)width, (int)height};
    int rc = ioctl(m_framebuffer_fd, 1985, (int)&resolution);
    ASSERT(rc == 0);

    if (m_framebuffer) {
        size_t previous_size_in_bytes = m_width * m_height * sizeof(RGBA32) * 2;
        int rc = munmap(m_framebuffer, previous_size_in_bytes);
        ASSERT(rc == 0);
    }

    size_t framebuffer_size_in_bytes = width * height * sizeof(RGBA32) * 2;
    m_framebuffer = (RGBA32*)mmap(nullptr, framebuffer_size_in_bytes, PROT_READ | PROT_WRITE, MAP_SHARED, m_framebuffer_fd, 0);
    ASSERT(m_framebuffer && m_framebuffer != (void*)-1);

    m_width = width;
    m_height = height;

    m_cursor_location.constrain(rect());
}

void WSScreen::on_receive_mouse_data(int dx, int dy, int dz, unsigned buttons)
{
    auto prev_location = m_cursor_location;
    m_cursor_location.move_by(dx, dy);
    m_cursor_location.constrain(rect());
    unsigned prev_buttons = m_mouse_button_state;
    m_mouse_button_state = buttons;
    unsigned changed_buttons = prev_buttons ^ buttons;
    auto post_mousedown_or_mouseup_if_needed = [&] (MouseButton button) {
        if (!(changed_buttons & (unsigned)button))
            return;
        auto message = make<WSMouseEvent>(buttons & (unsigned)button ? WSEvent::MouseDown : WSEvent::MouseUp, m_cursor_location, buttons, button, m_modifiers);
        WSEventLoop::the().post_event(WSWindowManager::the(), move(message));
    };
    post_mousedown_or_mouseup_if_needed(MouseButton::Left);
    post_mousedown_or_mouseup_if_needed(MouseButton::Right);
    post_mousedown_or_mouseup_if_needed(MouseButton::Middle);
    if (m_cursor_location != prev_location) {
        auto message = make<WSMouseEvent>(WSEvent::MouseMove, m_cursor_location, buttons, MouseButton::None, m_modifiers);
        WSEventLoop::the().post_event(WSWindowManager::the(), move(message));
    }

    if (dz) {
        auto message = make<WSMouseEvent>(WSEvent::MouseWheel, m_cursor_location, buttons, MouseButton::None, m_modifiers, dz);
        WSEventLoop::the().post_event(WSWindowManager::the(), move(message));
    }

    if (m_cursor_location != prev_location)
        WSCompositor::the().invalidate_cursor();
}

const char* KeyCodeName(enum KeyCode k);
const AK::String KeyEventName(KeyEvent e);

void WSScreen::on_receive_keyboard_data(KeyEvent kernel_event)
{
    dbgprintf("WSScreen::on_receive_keyboard_data: %s\n", KeyEventName(kernel_event).characters());

    if (kernel_event.is_press()) {
        if (m_key_state.get(kernel_event.key) == true) {
            dbgprintf("WSScreen: unexpected key down for %s\n", KeyCodeName(kernel_event.key));
        }

        m_key_state.set(kernel_event.key, true);
    } else {
        if (m_key_state.get(kernel_event.key) == false) {
            dbgprintf("WSScreen: unexpected key up for %s\n", KeyCodeName(kernel_event.key));
        }

        m_key_state.set(kernel_event.key, false);
    }

    m_modifiers = kernel_event.modifiers();
    auto message = make<WSKeyEvent>(kernel_event.is_press() ? WSEvent::KeyDown : WSEvent::KeyUp, kernel_event.key, kernel_event.character, kernel_event.modifiers());
    WSEventLoop::the().post_event(WSWindowManager::the(), move(message));
}

void WSScreen::set_y_offset(int offset)
{
    int rc = ioctl(m_framebuffer_fd, 1982, offset);
    ASSERT(rc == 0);
}

const char* KeyCodeName(enum KeyCode k)
{
    switch (k) {
    case Key_Invalid:
        return "Invalid";
    case Key_Escape:
        return "Escape";
    case Key_Tab:
        return "Tab";
    case Key_Backspace:
        return "Backspace";
    case Key_Return:
        return "Return";
    case Key_Insert:
        return "Insert";
    case Key_Delete:
        return "Delete";
    case Key_PrintScreen:
        return "PrintScreen";
    case Key_SysRq:
        return "SysRq";
    case Key_Home:
        return "Home";
    case Key_End:
        return "End";
    case Key_Left:
        return "Left";
    case Key_Up:
        return "Up";
    case Key_Right:
        return "Right";
    case Key_Down:
        return "Down";
    case Key_PageUp:
        return "PageUp";
    case Key_PageDown:
        return "PageDown";
    case Key_LeftShift:
        return "LeftShift";
    case Key_RightShift:
        return "RightShift";
    case Key_Control:
        return "Control";
    case Key_Alt:
        return "Alt";
    case Key_CapsLock:
        return "CapsLock";
    case Key_NumLock:
        return "NumLock";
    case Key_ScrollLock:
        return "ScrollLock";
    case Key_F1:
        return "F1";
    case Key_F2:
        return "F2";
    case Key_F3:
        return "F3";
    case Key_F4:
        return "F4";
    case Key_F5:
        return "F5";
    case Key_F6:
        return "F6";
    case Key_F7:
        return "F7";
    case Key_F8:
        return "F8";
    case Key_F9:
        return "F9";
    case Key_F10:
        return "F10";
    case Key_F11:
        return "F11";
    case Key_F12:
        return "F12";
    case Key_Space:
        return "Space";
    case Key_ExclamationPoint:
        return "ExclamationPoint";
    case Key_DoubleQuote:
        return "DoubleQuote";
    case Key_Hashtag:
        return "Hashtag";
    case Key_Dollar:
        return "Dollar";
    case Key_Percent:
        return "Percent";
    case Key_Ampersand:
        return "Ampersand";
    case Key_Apostrophe:
        return "Apostrophe";
    case Key_LeftParen:
        return "LeftParen";
    case Key_RightParen:
        return "RightParen";
    case Key_Asterisk:
        return "Asterisk";
    case Key_Plus:
        return "Plus";
    case Key_Comma:
        return "Comma";
    case Key_Minus:
        return "Minus";
    case Key_Period:
        return "Period";
    case Key_Slash:
        return "Slash";
    case Key_0:
        return "0";
    case Key_1:
        return "1";
    case Key_2:
        return "2";
    case Key_3:
        return "3";
    case Key_4:
        return "4";
    case Key_5:
        return "5";
    case Key_6:
        return "6";
    case Key_7:
        return "7";
    case Key_8:
        return "8";
    case Key_9:
        return "9";
    case Key_Colon:
        return "Colon";
    case Key_Semicolon:
        return "Semicolon";
    case Key_LessThan:
        return "LessThan";
    case Key_Equal:
        return "Equal";
    case Key_GreaterThan:
        return "GreaterThan";
    case Key_QuestionMark:
        return "QuestionMark";
    case Key_AtSign:
        return "AtSign";
    case Key_A:
        return "A";
    case Key_B:
        return "B";
    case Key_C:
        return "C";
    case Key_D:
        return "D";
    case Key_E:
        return "E";
    case Key_F:
        return "F";
    case Key_G:
        return "G";
    case Key_H:
        return "H";
    case Key_I:
        return "I";
    case Key_J:
        return "J";
    case Key_K:
        return "K";
    case Key_L:
        return "L";
    case Key_M:
        return "M";
    case Key_N:
        return "N";
    case Key_O:
        return "O";
    case Key_P:
        return "P";
    case Key_Q:
        return "Q";
    case Key_R:
        return "R";
    case Key_S:
        return "S";
    case Key_T:
        return "T";
    case Key_U:
        return "U";
    case Key_V:
        return "V";
    case Key_W:
        return "W";
    case Key_X:
        return "X";
    case Key_Y:
        return "Y";
    case Key_Z:
        return "Z";
    case Key_LeftBracket:
        return "LeftBracket";
    case Key_RightBracket:
        return "RightBracket";
    case Key_Backslash:
        return "Backslash";
    case Key_Circumflex:
        return "Circumflex";
    case Key_Underscore:
        return "Underscore";
    case Key_LeftBrace:
        return "LeftBrace";
    case Key_RightBrace:
        return "RightBrace";
    case Key_Pipe:
        return "Pipe";
    case Key_Tilde:
        return "Tilde";
    case Key_Backtick:
        return "Backtick";
    case Key_Logo:
        return "Logo";
    default:
        return "<<unknown>>";
    }
}

const AK::String KeyEventName(KeyEvent e)
{
    AK::StringBuilder b;

    if (e.is_press()) {
        b.append("[x] ");
    } else {
        b.append("[ ] ");
    }

    if (e.alt() && e.key != Key_Alt)
        b.append("Alt+");

    if (e.ctrl() && e.key != Key_Control)
        b.append("Ctrl+");

    if (e.shift() && e.key != Key_LeftShift && e.key != Key_RightShift)
        b.append("Shift+");

    if (e.logo() && e.key != Key_Logo)
        b.append("Logo+");

    b.append(KeyCodeName(e.key));

    return b.to_string();
}
