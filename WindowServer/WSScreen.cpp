#include "WSScreen.h"
#include "WSMessageLoop.h"
#include "WSMessage.h"
#include "WSWindowManager.h"
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>

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

    struct BXVGAResolution {
        int width;
        int height;
    };
    BXVGAResolution resolution { (int)width, (int)height};
    int rc = ioctl(m_framebuffer_fd, 1985, (int)&resolution);
    ASSERT(rc == 0);

    size_t framebuffer_size_in_bytes = resolution.width * resolution.height * sizeof(RGBA32) * 2;
    m_framebuffer = (RGBA32*)mmap(nullptr, framebuffer_size_in_bytes, PROT_READ | PROT_WRITE, MAP_SHARED, m_framebuffer_fd, 0);
    ASSERT(m_framebuffer && m_framebuffer != (void*)-1);
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

void WSScreen::on_receive_keyboard_data(KeyEvent kernel_event)
{
    auto message = make<WSKeyEvent>(kernel_event.is_press() ? WSMessage::KeyDown : WSMessage::KeyUp, kernel_event.key, kernel_event.character);
    message->m_shift = kernel_event.shift();
    message->m_ctrl = kernel_event.ctrl();
    message->m_alt = kernel_event.alt();
    WSMessageLoop::the().post_message(&WSWindowManager::the(), move(message));
}

void WSScreen::set_y_offset(int offset)
{
    int rc = ioctl(m_framebuffer_fd, 1982, offset);
    ASSERT(rc == 0);
}
