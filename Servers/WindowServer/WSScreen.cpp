#include "WSScreen.h"
#include "WSCompositor.h"
#include "WSEvent.h"
#include "WSEventLoop.h"
#include "WSWindowManager.h"
#include <Kernel/FB.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

static WSScreen* s_the;

WSScreen& WSScreen::the()
{
    ASSERT(s_the);
    return *s_the;
}

WSScreen::WSScreen(unsigned desired_width, unsigned desired_height)
{
    ASSERT(!s_the);
    s_the = this;
    m_framebuffer_fd = open("/dev/fb0", O_RDWR | O_CLOEXEC);
    ASSERT(m_framebuffer_fd >= 0);

    if (fb_set_buffer(m_framebuffer_fd, 0) == 0) {
        m_can_set_buffer = true;
    }

    set_resolution(desired_width, desired_height);
    m_cursor_location = rect().center();
}

WSScreen::~WSScreen()
{
}

void WSScreen::set_resolution(int width, int height)
{
    FBResolution resolution { 0, (int)width, (int)height };
    int rc = fb_set_resolution(m_framebuffer_fd, &resolution);
    ASSERT(rc == 0);
    on_change_resolution(resolution.pitch, resolution.width, resolution.height);
}

void WSScreen::on_change_resolution(int pitch, int width, int height)
{
    if (m_framebuffer) {
        size_t previous_size_in_bytes = m_size_in_bytes;
        int rc = munmap(m_framebuffer, previous_size_in_bytes);
        ASSERT(rc == 0);
    }

    int rc = fb_get_size_in_bytes(m_framebuffer_fd, &m_size_in_bytes);
    ASSERT(rc == 0);

    m_framebuffer = (RGBA32*)mmap(nullptr, m_size_in_bytes, PROT_READ | PROT_WRITE, MAP_SHARED, m_framebuffer_fd, 0);
    ASSERT(m_framebuffer && m_framebuffer != (void*)-1);

    m_pitch = pitch;
    m_width = width;
    m_height = height;

    m_cursor_location.constrain(rect());
}

void WSScreen::set_buffer(int index)
{
    ASSERT(m_can_set_buffer);
    int rc = fb_set_buffer(m_framebuffer_fd, index);
    ASSERT(rc == 0);
}

void WSScreen::on_receive_mouse_data(int dx, int dy, int dz, unsigned buttons)
{
    auto prev_location = m_cursor_location;
    m_cursor_location.move_by(dx, dy);
    m_cursor_location.constrain(rect());
    unsigned prev_buttons = m_mouse_button_state;
    m_mouse_button_state = buttons;
    unsigned changed_buttons = prev_buttons ^ buttons;
    auto post_mousedown_or_mouseup_if_needed = [&](MouseButton button) {
        if (!(changed_buttons & (unsigned)button))
            return;
        auto message = make<WSMouseEvent>(buttons & (unsigned)button ? WSEvent::MouseDown : WSEvent::MouseUp, m_cursor_location, buttons, button, m_modifiers);
        CEventLoop::current().post_event(WSWindowManager::the(), move(message));
    };
    post_mousedown_or_mouseup_if_needed(MouseButton::Left);
    post_mousedown_or_mouseup_if_needed(MouseButton::Right);
    post_mousedown_or_mouseup_if_needed(MouseButton::Middle);
    if (m_cursor_location != prev_location) {
        auto message = make<WSMouseEvent>(WSEvent::MouseMove, m_cursor_location, buttons, MouseButton::None, m_modifiers);
        CEventLoop::current().post_event(WSWindowManager::the(), move(message));
    }

    if (dz) {
        auto message = make<WSMouseEvent>(WSEvent::MouseWheel, m_cursor_location, buttons, MouseButton::None, m_modifiers, dz);
        CEventLoop::current().post_event(WSWindowManager::the(), move(message));
    }

    if (m_cursor_location != prev_location)
        WSCompositor::the().invalidate_cursor();
}

void WSScreen::on_receive_keyboard_data(KeyEvent kernel_event)
{
    m_modifiers = kernel_event.modifiers();
    auto message = make<WSKeyEvent>(kernel_event.is_press() ? WSEvent::KeyDown : WSEvent::KeyUp, kernel_event.key, kernel_event.character, kernel_event.modifiers());
    CEventLoop::current().post_event(WSWindowManager::the(), move(message));
}
