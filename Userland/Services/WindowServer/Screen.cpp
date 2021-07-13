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
ScreenLayout Screen::s_layout;
Vector<int, default_scale_factors_in_use_count> Screen::s_scale_factors_in_use;

struct ScreenFBData {
    Vector<FBRect, 32> pending_flush_rects;
    bool too_many_pending_flush_rects { false };
};

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

bool Screen::apply_layout(ScreenLayout&& screen_layout, String& error_msg)
{
    if (!screen_layout.is_valid(&error_msg))
        return false;

    auto screens_backup = move(s_screens);
    auto layout_backup = move(s_layout);
    for (auto& old_screen : screens_backup)
        old_screen.close_device();

    AK::ArmedScopeGuard rollback([&] {
        for (auto& screen : s_screens)
            screen.close_device();
        s_screens = move(screens_backup);
        s_layout = move(layout_backup);
        for (auto& old_screen : screens_backup) {
            if (!old_screen.open_device()) {
                // Don't set error_msg here, it should already be set
                dbgln("Rolling back screen layout failed: could not open device");
            }
        }
        update_bounding_rect();
    });
    s_layout = move(screen_layout);
    for (size_t index = 0; index < s_layout.screens.size(); index++) {
        auto* screen = WindowServer::Screen::create(s_layout.screens[index]);
        if (!screen) {
            error_msg = String::formatted("Error creating screen #{}", index);
            return false;
        }

        if (s_layout.main_screen_index == index)
            screen->make_main_screen();

        screen->open_device();
    }

    rollback.disarm();
    update_bounding_rect();
    update_scale_factors_in_use();
    return true;
}

void Screen::update_scale_factors_in_use()
{
    s_scale_factors_in_use.clear();
    for_each([&](auto& screen) {
        auto scale_factor = screen.scale_factor();
        // The This doesn't have to be extremely efficient as this
        // code is only run when we start up or the screen configuration
        // changes. But using a vector allows for efficient iteration,
        // which is the most common use case.
        if (!s_scale_factors_in_use.contains_slow(scale_factor))
            s_scale_factors_in_use.append(scale_factor);
        return IterationDecision::Continue;
    });
}

Screen::Screen(ScreenLayout::Screen& screen_info)
    : m_virtual_rect(screen_info.location, { screen_info.resolution.width() / screen_info.scale_factor, screen_info.resolution.height() / screen_info.scale_factor })
    , m_framebuffer_data(adopt_own(*new ScreenFBData()))
    , m_info(screen_info)
{
    open_device();

    // If the cursor is not in a valid screen (yet), force it into one
    dbgln("Screen() current physical cursor location: {} rect: {}", ScreenInput::the().cursor_location(), rect());
    if (!find_by_location(ScreenInput::the().cursor_location()))
        ScreenInput::the().set_cursor_location(rect().center());
}

Screen::~Screen()
{
    close_device();
}

bool Screen::open_device()
{
    close_device();
    m_framebuffer_fd = open(m_info.device.characters(), O_RDWR | O_CLOEXEC);
    if (m_framebuffer_fd < 0) {
        perror(String::formatted("failed to open {}", m_info.device).characters());
        return false;
    }

    m_can_set_buffer = (fb_set_buffer(m_framebuffer_fd, 0) == 0);
    m_can_device_flush_buffers = true; // If the device can't do it we revert to false
    set_resolution(true);
    return true;
}

void Screen::close_device()
{
    if (m_framebuffer_fd >= 0) {
        close(m_framebuffer_fd);
        m_framebuffer_fd = -1;
    }
    if (m_framebuffer) {
        int rc = munmap(m_framebuffer, m_size_in_bytes);
        VERIFY(rc == 0);

        m_framebuffer = nullptr;
        m_size_in_bytes = 0;
    }
}

void Screen::init()
{
    set_resolution(true);
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

bool Screen::set_resolution(bool initial)
{
    // Remember the screen that the cursor is on. Make sure it stays on the same screen if we change its resolution...
    Screen* screen_with_cursor = nullptr;
    if (!initial)
        screen_with_cursor = &ScreenInput::the().cursor_location_screen();

    FBResolution physical_resolution { 0, (unsigned)m_info.resolution.width(), (unsigned)m_info.resolution.height() };
    int rc = fb_set_resolution(m_framebuffer_fd, &physical_resolution);
    dbgln_if(WSSCREEN_DEBUG, "Screen #{}: fb_set_resolution() - return code {}", index(), rc);

    auto on_change_resolution = [&]() {
        if (initial || physical_resolution.width != (unsigned)m_info.resolution.width() || physical_resolution.height != (unsigned)m_info.resolution.height()) {
            if (m_framebuffer) {
                size_t previous_size_in_bytes = m_size_in_bytes;
                int rc = munmap(m_framebuffer, previous_size_in_bytes);
                VERIFY(rc == 0);
            }

            int rc = fb_get_size_in_bytes(m_framebuffer_fd, &m_size_in_bytes);
            VERIFY(rc == 0);

            m_framebuffer = (Gfx::RGBA32*)mmap(nullptr, m_size_in_bytes, PROT_READ | PROT_WRITE, MAP_SHARED, m_framebuffer_fd, 0);
            VERIFY(m_framebuffer && m_framebuffer != (void*)-1);

            if (m_can_set_buffer) {
                unsigned buffer_offset = 0;
                rc = fb_get_buffer_offset(m_framebuffer_fd, 1, &buffer_offset);
                if (rc == 0) {
                    m_back_buffer_offset = buffer_offset;
                } else {
                    // fall back to assuming the second buffer starts right after the last line of the first
                    m_back_buffer_offset = physical_resolution.pitch * physical_resolution.height;
                }
            } else {
                m_back_buffer_offset = 0;
            }
        }

        m_info.resolution = { physical_resolution.width, physical_resolution.height };
        m_pitch = physical_resolution.pitch;
        if (this == screen_with_cursor) {
            auto& screen_input = ScreenInput::the();
            screen_input.set_cursor_location(screen_input.cursor_location().constrained(rect()));
        }
    };

    if (rc == 0) {
        on_change_resolution();
        return true;
    }
    if (rc == -1) {
        int err = errno;
        dbgln("Screen #{}: Failed to set resolution {}: {}", index(), m_info.resolution, strerror(err));
        on_change_resolution();
        return false;
    }
    VERIFY_NOT_REACHED();
}

void Screen::set_buffer(int index)
{
    VERIFY(m_can_set_buffer);
    int rc = fb_set_buffer(m_framebuffer_fd, index);
    VERIFY(rc == 0);
}

size_t Screen::buffer_offset(int index) const
{
    if (index == 0)
        return 0;
    if (index == 1)
        return m_back_buffer_offset;
    VERIFY_NOT_REACHED();
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
        m_cursor_location = { packet.x * current_screen.width() / 0xffff, packet.y * current_screen.height() / 0xffff };
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

void Screen::queue_flush_display_rect(Gfx::IntRect const& flush_region)
{
    // NOTE: we don't scale until in Screen::flush_display so that when
    // there are too many rectangles that we end up throwing away, we didn't
    // waste accounting for scale factor!
    auto& fb_data = *m_framebuffer_data;
    if (fb_data.too_many_pending_flush_rects) {
        // We already have too many, just make sure we extend it if needed
        VERIFY(!fb_data.pending_flush_rects.is_empty());
        if (fb_data.pending_flush_rects.size() == 1) {
            auto& union_rect = fb_data.pending_flush_rects[0];
            auto new_union = flush_region.united(Gfx::IntRect((int)union_rect.x, (int)union_rect.y, (int)union_rect.width, (int)union_rect.height));
            union_rect.x = new_union.left();
            union_rect.y = new_union.top();
            union_rect.width = new_union.width();
            union_rect.height = new_union.height();
        } else {
            // Convert all the rectangles into one union
            auto new_union = flush_region;
            for (auto& flush_rect : fb_data.pending_flush_rects)
                new_union = new_union.united(Gfx::IntRect((int)flush_rect.x, (int)flush_rect.y, (int)flush_rect.width, (int)flush_rect.height));
            fb_data.pending_flush_rects.resize(1, true);
            auto& union_rect = fb_data.pending_flush_rects[0];
            union_rect.x = new_union.left();
            union_rect.y = new_union.top();
            union_rect.width = new_union.width();
            union_rect.height = new_union.height();
        }
        return;
    }
    VERIFY(fb_data.pending_flush_rects.size() < fb_data.pending_flush_rects.capacity());
    fb_data.pending_flush_rects.append({ (unsigned)flush_region.left(),
        (unsigned)flush_region.top(),
        (unsigned)flush_region.width(),
        (unsigned)flush_region.height() });
    if (fb_data.pending_flush_rects.size() == fb_data.pending_flush_rects.capacity()) {
        // If we get one more rectangle then we need to convert it to a single union rectangle
        fb_data.too_many_pending_flush_rects = true;
    }
}

void Screen::flush_display(int buffer_index)
{
    VERIFY(m_can_device_flush_buffers);
    auto& fb_data = *m_framebuffer_data;
    if (fb_data.pending_flush_rects.is_empty())
        return;

    // Now that we have a final set of rects, apply the scale factor
    auto scale_factor = this->scale_factor();
    for (auto& flush_rect : fb_data.pending_flush_rects) {
        flush_rect.x *= scale_factor;
        flush_rect.y *= scale_factor;
        flush_rect.width *= scale_factor;
        flush_rect.height *= scale_factor;
    }

    if (fb_flush_buffers(m_framebuffer_fd, buffer_index, fb_data.pending_flush_rects.data(), (unsigned)fb_data.pending_flush_rects.size()) < 0) {
        int err = errno;
        if (err == ENOTSUP)
            m_can_device_flush_buffers = false;
        else
            dbgln("Screen #{}: Error ({}) flushing display: {}", index(), err, strerror(err));
    }

    fb_data.too_many_pending_flush_rects = false;
    fb_data.pending_flush_rects.clear_with_capacity();
}

void Screen::flush_display_front_buffer(int front_buffer_index, Gfx::IntRect& rect)
{
    VERIFY(m_can_device_flush_buffers);
    auto scale_factor = this->scale_factor();
    FBRect flush_rect {
        .x = (unsigned)(rect.x() * scale_factor),
        .y = (unsigned)(rect.y() * scale_factor),
        .width = (unsigned)(rect.width() * scale_factor),
        .height = (unsigned)(rect.height() * scale_factor)
    };

    if (fb_flush_buffers(m_framebuffer_fd, front_buffer_index, &flush_rect, 1) < 0) {
        int err = errno;
        if (err == ENOTSUP)
            m_can_device_flush_buffers = false;
        else
            dbgln("Screen #{}: Error ({}) flushing display front buffer: {}", index(), err, strerror(err));
    }
}

}
