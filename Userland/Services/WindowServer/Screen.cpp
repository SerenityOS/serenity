/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Screen.h"
#include "Compositor.h"
#include "Event.h"
#include "EventLoop.h"
#include "WindowManager.h"
#include <AK/Debug.h>
#include <Kernel/API/FB.h>
#include <Kernel/API/MousePacket.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>
#include <unistd.h>

namespace WindowServer {

NonnullOwnPtrVector<Screen, default_screen_count> Screen::s_screens;
Screen* Screen::s_main_screen { nullptr };
Gfx::IntRect Screen::s_bounding_screens_rect {};

ScreenInput& ScreenInput::the()
{
    static ScreenInput s_the;
    return s_the;
}

Screen& ScreenInput::cursor_location_screen()
{
    auto* screen = Screen::find_by_location(m_cursor_location);
    VERIFY(screen);
    return *screen;
}

const Screen& ScreenInput::cursor_location_screen() const
{
    auto* screen = Screen::find_by_location(m_cursor_location);
    VERIFY(screen);
    return *screen;
}

Screen::Screen(const String& device, const Gfx::IntRect& virtual_rect, int scale_factor)
    : m_virtual_rect(virtual_rect)
    , m_scale_factor(scale_factor)
{
    m_framebuffer_fd = open(device.characters(), O_RDWR | O_CLOEXEC);
    if (m_framebuffer_fd < 0) {
        perror(String::formatted("failed to open {}", device).characters());
        VERIFY_NOT_REACHED();
    }

    if (fb_set_buffer(m_framebuffer_fd, 0) == 0) {
        m_can_set_buffer = true;
    }

    // If the cursor is not in a valid screen (yet), force it into one
    dbgln("Screen() current physical cursor location: {} rect: {}", ScreenInput::the().cursor_location(), rect());
    if (!find_by_location(ScreenInput::the().cursor_location()))
        ScreenInput::the().set_cursor_location(rect().center());
}

Screen::~Screen()
{
    close(m_framebuffer_fd);
}

void Screen::init()
{
    do_set_resolution(true, m_virtual_rect.width(), m_virtual_rect.height(), m_scale_factor);
}

Screen& Screen::closest_to_rect(const Gfx::IntRect& rect)
{
    Screen* best_screen = nullptr;
    int best_area = 0;
    for (auto& screen : s_screens) {
        auto r = screen.rect().intersected(rect);
        int area = r.width() * r.height();
        if (!best_screen || area > best_area) {
            best_screen = &screen;
            best_area = area;
        }
    }
    if (!best_screen) {
        // TODO: try to find the best screen in close proximity
        best_screen = &Screen::main();
    }
    return *best_screen;
}

Screen& Screen::closest_to_location(const Gfx::IntPoint& point)
{
    for (auto& screen : s_screens) {
        if (screen.rect().contains(point))
            return screen;
    }
    // TODO: guess based on how close the point is to the next screen rectangle
    return Screen::main();
}

void Screen::update_bounding_rect()
{
    if (!s_screens.is_empty()) {
        s_bounding_screens_rect = s_screens[0].rect();
        for (size_t i = 1; i < s_screens.size(); i++)
            s_bounding_screens_rect = s_bounding_screens_rect.united(s_screens[i].rect());
    } else {
        s_bounding_screens_rect = {};
    }
}

bool Screen::do_set_resolution(bool initial, int width, int height, int new_scale_factor)
{
    // Remember the screen that the cursor is on. Make sure it stays on the same screen if we change its resolution...
    auto& screen_with_cursor = ScreenInput::the().cursor_location_screen();

    int new_physical_width = width * new_scale_factor;
    int new_physical_height = height * new_scale_factor;
    if (!initial && physical_width() == new_physical_width && physical_height() == new_physical_height) {
        VERIFY(initial || scale_factor() != new_scale_factor);
        on_change_resolution(initial, m_pitch, physical_width(), physical_height(), new_scale_factor, screen_with_cursor);
        return true;
    }

    FBResolution physical_resolution { 0, (unsigned)new_physical_width, (unsigned)new_physical_height };
    int rc = fb_set_resolution(m_framebuffer_fd, &physical_resolution);
    dbgln_if(WSSCREEN_DEBUG, "Screen #{}: fb_set_resolution() - return code {}", index(), rc);

    if (rc == 0) {
        on_change_resolution(initial, physical_resolution.pitch, physical_resolution.width, physical_resolution.height, new_scale_factor, screen_with_cursor);
        return true;
    }
    if (rc == -1) {
        int err = errno;
        dbgln("Screen #{}: Failed to set resolution {}x{}: {}", index(), width, height, strerror(err));
        on_change_resolution(initial, physical_resolution.pitch, physical_resolution.width, physical_resolution.height, new_scale_factor, screen_with_cursor);
        return false;
    }
    VERIFY_NOT_REACHED();
}

void Screen::on_change_resolution(bool initial, int pitch, int new_physical_width, int new_physical_height, int new_scale_factor, Screen& screen_with_cursor)
{
    if (initial || physical_width() != new_physical_width || physical_height() != new_physical_height) {
        if (m_framebuffer) {
            size_t previous_size_in_bytes = m_size_in_bytes;
            int rc = munmap(m_framebuffer, previous_size_in_bytes);
            VERIFY(rc == 0);
        }

        int rc = fb_get_size_in_bytes(m_framebuffer_fd, &m_size_in_bytes);
        VERIFY(rc == 0);

        m_framebuffer = (Gfx::RGBA32*)mmap(nullptr, m_size_in_bytes, PROT_READ | PROT_WRITE, MAP_SHARED, m_framebuffer_fd, 0);
        VERIFY(m_framebuffer && m_framebuffer != (void*)-1);
    }

    m_pitch = pitch;
    m_virtual_rect.set_width(new_physical_width / new_scale_factor);
    m_virtual_rect.set_height(new_physical_height / new_scale_factor);
    m_scale_factor = new_scale_factor;
    update_bounding_rect();

    if (this == &screen_with_cursor) {
        auto& screen_input = ScreenInput::the();
        screen_input.set_cursor_location(screen_input.cursor_location().constrained(rect()));
    }
}

void Screen::set_buffer(int index)
{
    VERIFY(m_can_set_buffer);
    int rc = fb_set_buffer(m_framebuffer_fd, index);
    VERIFY(rc == 0);
}

void ScreenInput::set_acceleration_factor(double factor)
{
    VERIFY(factor >= mouse_accel_min && factor <= mouse_accel_max);
    m_acceleration_factor = factor;
}

void ScreenInput::set_scroll_step_size(unsigned step_size)
{
    VERIFY(step_size >= scroll_step_size_min);
    m_scroll_step_size = step_size;
}

void ScreenInput::on_receive_mouse_data(const MousePacket& packet)
{
    auto& current_screen = cursor_location_screen();
    auto prev_location = m_cursor_location;
    if (packet.is_relative) {
        m_cursor_location.translate_by(packet.x * m_acceleration_factor, packet.y * m_acceleration_factor);
        dbgln_if(WSSCREEN_DEBUG, "Screen: New Relative mouse point @ {}", m_cursor_location);
    } else {
        m_cursor_location = { packet.x * current_screen.physical_width() / 0xffff, packet.y * current_screen.physical_height() / 0xffff };
        dbgln_if(WSSCREEN_DEBUG, "Screen: New Absolute mouse point @ {}", m_cursor_location);
    }

    auto* moved_to_screen = Screen::find_by_location(m_cursor_location);
    if (!moved_to_screen) {
        m_cursor_location = m_cursor_location.constrained(current_screen.rect());
        moved_to_screen = &current_screen;
    }

    unsigned buttons = packet.buttons;
    unsigned prev_buttons = m_mouse_button_state;
    m_mouse_button_state = buttons;
    unsigned changed_buttons = prev_buttons ^ buttons;
    auto post_mousedown_or_mouseup_if_needed = [&](MouseButton button) {
        if (!(changed_buttons & (unsigned)button))
            return;
        auto message = make<MouseEvent>(buttons & (unsigned)button ? Event::MouseDown : Event::MouseUp, m_cursor_location, buttons, button, m_modifiers);
        Core::EventLoop::current().post_event(WindowManager::the(), move(message));
    };
    post_mousedown_or_mouseup_if_needed(MouseButton::Left);
    post_mousedown_or_mouseup_if_needed(MouseButton::Right);
    post_mousedown_or_mouseup_if_needed(MouseButton::Middle);
    post_mousedown_or_mouseup_if_needed(MouseButton::Back);
    post_mousedown_or_mouseup_if_needed(MouseButton::Forward);
    if (m_cursor_location != prev_location) {
        auto message = make<MouseEvent>(Event::MouseMove, m_cursor_location, buttons, MouseButton::None, m_modifiers);
        if (WindowManager::the().dnd_client())
            message->set_mime_data(WindowManager::the().dnd_mime_data());
        Core::EventLoop::current().post_event(WindowManager::the(), move(message));
    }

    if (packet.z) {
        auto message = make<MouseEvent>(Event::MouseWheel, m_cursor_location, buttons, MouseButton::None, m_modifiers, packet.z * m_scroll_step_size);
        Core::EventLoop::current().post_event(WindowManager::the(), move(message));
    }

    if (m_cursor_location != prev_location)
        Compositor::the().invalidate_cursor();
}

void ScreenInput::on_receive_keyboard_data(::KeyEvent kernel_event)
{
    m_modifiers = kernel_event.modifiers();
    auto message = make<KeyEvent>(kernel_event.is_press() ? Event::KeyDown : Event::KeyUp, kernel_event.key, kernel_event.code_point, kernel_event.modifiers(), kernel_event.scancode);
    Core::EventLoop::current().post_event(WindowManager::the(), move(message));
}

}
