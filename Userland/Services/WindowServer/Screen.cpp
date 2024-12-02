/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Screen.h"
#include "Compositor.h"
#include "Event.h"
#include "ScreenBackend.h"
#include "VirtualScreenBackend.h"
#include "WindowManager.h"
#include <AK/Debug.h>
#include <AK/Format.h>
#include <Kernel/API/MousePacket.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/devices/gpu.h>
#include <sys/mman.h>
#include <unistd.h>

namespace WindowServer {

Vector<NonnullRefPtr<Screen>, default_screen_count> Screen::s_screens;
Screen* Screen::s_main_screen { nullptr };
Gfx::IntRect Screen::s_bounding_screens_rect {};
ScreenLayout Screen::s_layout;
Vector<int, default_scale_factors_in_use_count> Screen::s_scale_factors_in_use;

struct FlushRectData {
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

Screen const& ScreenInput::cursor_location_screen() const
{
    auto* screen = Screen::find_by_location(m_cursor_location);
    VERIFY(screen);
    return *screen;
}

bool Screen::apply_layout(ScreenLayout&& screen_layout, ByteString& error_msg)
{
    if (!screen_layout.is_valid(&error_msg))
        return false;

    if (screen_layout == s_layout)
        return true;

    bool place_cursor_on_main_screen = find_by_location(ScreenInput::the().cursor_location()) == nullptr;

    HashMap<size_t, size_t> current_to_new_indices_map;
    HashMap<size_t, size_t> new_to_current_indices_map;
    HashMap<size_t, NonnullRefPtr<Screen>> devices_no_longer_used;
    for (size_t i = 0; i < s_layout.screens.size(); i++) {
        auto& screen = s_layout.screens[i];
        bool found = false;
        for (size_t j = 0; j < screen_layout.screens.size(); j++) {
            auto& new_screen = screen_layout.screens[j];
            if (new_screen.device == screen.device) {
                current_to_new_indices_map.set(i, j);
                new_to_current_indices_map.set(j, i);
                found = true;
                break;
            }
        }

        if (!found)
            devices_no_longer_used.set(i, s_screens[i]);
    }
    HashMap<Screen*, size_t> screens_with_resolution_change;
    HashMap<Screen*, size_t> screens_with_scale_change;
    for (auto [current_index, new_index] : current_to_new_indices_map) {
        auto& screen = s_layout.screens[current_index];
        auto& new_screen = screen_layout.screens[new_index];
        if (screen.resolution != new_screen.resolution)
            screens_with_resolution_change.set(s_screens[current_index], new_index);
        if (screen.scale_factor != new_screen.scale_factor)
            screens_with_scale_change.set(s_screens[current_index], new_index);
    }

    auto screens_backup = move(s_screens);
    auto layout_backup = move(s_layout);

    for (auto& [existing_screen, _] : screens_with_resolution_change) {
        dbgln("Closing device {} in preparation for resolution change", layout_backup.screens[existing_screen->index()].device.value_or("<virtual screen>"));
        existing_screen->close_device();
    }

    AK::ArmedScopeGuard rollback([&] {
        for (auto& screen : s_screens)
            screen->close_device();
        s_screens = move(screens_backup);
        s_layout = move(layout_backup);
        for (size_t i = 0; i < s_screens.size(); i++) {
            auto& old_screen = s_screens[i];
            // Restore the original screen index in case it changed
            old_screen->set_index(i);
            if (i == s_layout.main_screen_index)
                old_screen->make_main_screen();
            bool changed_scale = screens_with_scale_change.contains(old_screen);
            if (screens_with_resolution_change.contains(old_screen)) {
                if (old_screen->open_device()) {
                    // The resolution was changed, so we also implicitly applied the new scale factor
                    changed_scale = false;
                } else {
                    // Don't set error_msg here, it should already be set
                    dbgln("Rolling back screen layout failed: could not open device");
                }
            }

            old_screen->update_virtual_and_physical_rects();
            if (changed_scale)
                old_screen->scale_factor_changed();
        }
        update_bounding_rect();
    });
    s_layout = move(screen_layout);
    for (size_t index = 0; index < s_layout.screens.size(); index++) {
        Screen* screen;
        bool need_to_open_device;
        if (auto it = new_to_current_indices_map.find(index); it != new_to_current_indices_map.end()) {
            // Re-use the existing screen instance
            screen = screens_backup[it->value];
            s_screens.append(*screen);
            screen->set_index(index);

            need_to_open_device = screens_with_resolution_change.contains(screen);
        } else {
            screen = WindowServer::Screen::create(index);
            if (!screen) {
                error_msg = ByteString::formatted("Error creating screen #{}", index);
                return false;
            }

            need_to_open_device = false;
        }

        if (need_to_open_device && !screen->open_device()) {
            error_msg = ByteString::formatted("Error opening device for screen #{}", index);
            return false;
        }

        screen->update_virtual_and_physical_rects();
        if (!need_to_open_device && screens_with_scale_change.contains(screen))
            screen->scale_factor_changed();

        VERIFY(screen);
        VERIFY(index == screen->index());

        if (s_layout.main_screen_index == index)
            screen->make_main_screen();
    }

    rollback.disarm();

    if (place_cursor_on_main_screen) {
        ScreenInput::the().set_cursor_location(Screen::main().rect().center());
    } else {
        auto cursor_location = ScreenInput::the().cursor_location();
        if (!find_by_location(cursor_location)) {
            // Cursor is off screen, try to find the closest location on another screen
            float closest_distance = 0;
            Optional<Gfx::IntPoint> closest_point;
            for (auto& screen : s_screens) {
                auto closest_point_on_screen_rect = screen->rect().closest_to(cursor_location);
                auto distance = closest_point_on_screen_rect.distance_from(cursor_location);
                if (!closest_point.has_value() || distance < closest_distance) {
                    closest_distance = distance;
                    closest_point = closest_point_on_screen_rect;
                }
            }
            ScreenInput::the().set_cursor_location(closest_point.value()); // We should always have one
        }
    }

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

Screen::Screen(size_t screen_index)
    : m_index(screen_index)
    , m_flush_rects(adopt_own(*new FlushRectData()))
    , m_compositor_screen_data(Compositor::create_screen_data({}))
{
    update_virtual_and_physical_rects();
    open_device();
}

Screen::~Screen()
{
    close_device();
}

bool Screen::open_device()
{
    close_device();
    auto& info = screen_layout_info();

    switch (info.mode) {
    case ScreenLayout::Screen::Mode::Device: {
        m_backend = make<HardwareScreenBackend>(info.device.value());
        auto return_value = m_backend->open();
        if (return_value.is_error()) {
            dbgln("Screen #{}: Failed to open backend: {}", index(), return_value.error());
            return false;
        }

        set_resolution(true);
        return true;
    }
    case ScreenLayout::Screen::Mode::Virtual: {
        m_backend = make<VirtualScreenBackend>();
        // Virtual device open should never fail.
        MUST(m_backend->open());

        set_resolution(true);
        return true;
    }
    default:
        dbgln("Unsupported screen type {}", ScreenLayout::Screen::mode_to_string(info.mode));
        return false;
    }
}

void Screen::close_device()
{
    m_backend = nullptr;
}

void Screen::update_virtual_and_physical_rects()
{
    auto& screen_info = screen_layout_info();
    m_virtual_rect = { screen_info.location, { screen_info.resolution.width() / screen_info.scale_factor, screen_info.resolution.height() / screen_info.scale_factor } };
    m_physical_rect = { Gfx::IntPoint { 0, 0 }, { screen_info.resolution.width(), screen_info.resolution.height() } };
    dbgln("update_virtual_and_physical_rects for screen #{}: {}", index(), m_virtual_rect);
}

void Screen::scale_factor_changed()
{
    // Flush rects are affected by the screen factor
    constrain_pending_flush_rects();
}

Screen& Screen::closest_to_rect(Gfx::IntRect const& rect)
{
    Screen* best_screen = nullptr;
    int best_area = 0;
    for (auto& screen : s_screens) {
        auto r = screen->rect().intersected(rect);
        int area = r.width() * r.height();
        if (!best_screen || area > best_area) {
            best_screen = screen;
            best_area = area;
        }
    }
    if (!best_screen) {
        // TODO: try to find the best screen in close proximity
        best_screen = &Screen::main();
    }
    return *best_screen;
}

Screen& Screen::closest_to_location(Gfx::IntPoint point)
{
    for (auto& screen : s_screens) {
        if (screen->rect().contains(point))
            return screen;
    }
    // TODO: guess based on how close the point is to the next screen rectangle
    return Screen::main();
}

void Screen::update_bounding_rect()
{
    if (!s_screens.is_empty()) {
        s_bounding_screens_rect = s_screens[0]->rect();
        for (size_t i = 1; i < s_screens.size(); i++)
            s_bounding_screens_rect = s_bounding_screens_rect.united(s_screens[i]->rect());
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

    auto& info = screen_layout_info();

    ErrorOr<void> return_value = Error::from_errno(EINVAL);
    {
        GraphicsHeadModeSetting requested_mode_setting;
        memset(&requested_mode_setting, 0, sizeof(GraphicsHeadModeSetting));
        requested_mode_setting.horizontal_stride = info.resolution.width() * 4;
        requested_mode_setting.pixel_clock_in_khz = 0;
        requested_mode_setting.horizontal_active = info.resolution.width();
        requested_mode_setting.horizontal_front_porch_pixels = 0;
        requested_mode_setting.horizontal_sync_time_pixels = 0;
        requested_mode_setting.horizontal_blank_pixels = 0;
        requested_mode_setting.vertical_active = info.resolution.height();
        requested_mode_setting.vertical_front_porch_lines = 0;
        requested_mode_setting.vertical_sync_time_lines = 0;
        requested_mode_setting.vertical_blank_lines = 0;
        requested_mode_setting.horizontal_offset = 0;
        requested_mode_setting.vertical_offset = 0;
        return_value = m_backend->set_head_mode_setting(requested_mode_setting);
    }

    dbgln_if(WSSCREEN_DEBUG, "Screen #{}: fb_set_resolution() - success", index());

    auto on_change_resolution = [&]() -> ErrorOr<void> {
        if (initial) {
            TRY(m_backend->unmap_framebuffer());
            TRY(m_backend->map_framebuffer());
        }
        auto mode_setting = TRY(m_backend->get_head_mode_setting());
        info.resolution = { mode_setting.horizontal_active, mode_setting.vertical_active };

        update_virtual_and_physical_rects();

        // Since pending flush rects are affected by the scale factor
        // update even if only the scale factor changed
        constrain_pending_flush_rects();

        if (this == screen_with_cursor) {
            auto& screen_input = ScreenInput::the();
            screen_input.set_cursor_location(screen_input.cursor_location().constrained(rect()));
        }
        return {};
    };

    if (!return_value.is_error()) {
        return_value = on_change_resolution();
        if (!return_value.is_error())
            return true;
    }
    if (return_value.is_error() && return_value.error() != Error::from_errno(EOVERFLOW)) {
        dbgln("Screen #{}: Failed to set resolution {}: {}", index(), info.resolution, return_value.error());
        MUST(on_change_resolution());
        return false;
    }
    dbgln("Screen #{}: Failed to set resolution {}: {}, falling back to safe resolution", index(), info.resolution, return_value.error());
    MUST(m_backend->set_safe_head_mode_setting());
    MUST(on_change_resolution());
    return false;
}

void Screen::set_buffer(int index)
{
    m_backend->set_head_buffer(index);
}

size_t Screen::buffer_offset(int index) const
{
    if (index == 0)
        return 0;
    if (index == 1)
        return m_backend->m_back_buffer_offset;
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

void ScreenInput::on_receive_mouse_data(MousePacket const& packet)
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
        m_cursor_location.constrain(current_screen.rect());
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
    post_mousedown_or_mouseup_if_needed(MouseButton::Primary);
    post_mousedown_or_mouseup_if_needed(MouseButton::Secondary);
    post_mousedown_or_mouseup_if_needed(MouseButton::Middle);
    post_mousedown_or_mouseup_if_needed(MouseButton::Backward);
    post_mousedown_or_mouseup_if_needed(MouseButton::Forward);

    if (m_cursor_location != prev_location) {
        auto message = make<MouseEvent>(Event::MouseMove, m_cursor_location, buttons, MouseButton::None, m_modifiers);
        Core::EventLoop::current().post_event(WindowManager::the(), move(message));
    }

    if (packet.z || packet.w) {
        auto message = make<MouseEvent>(Event::MouseWheel, m_cursor_location, buttons, MouseButton::None, m_modifiers, packet.w * m_scroll_step_size, packet.z * m_scroll_step_size, packet.w, packet.z);
        Core::EventLoop::current().post_event(WindowManager::the(), move(message));
    }

    if (m_cursor_location != prev_location)
        Compositor::the().invalidate_cursor();
}

void ScreenInput::on_receive_keyboard_data(::KeyEvent kernel_event)
{
    m_modifiers = kernel_event.modifiers();
    auto message = make<KeyEvent>(kernel_event.is_press() ? Event::KeyDown : Event::KeyUp, kernel_event.key, kernel_event.map_entry_index, kernel_event.code_point, kernel_event.modifiers(), kernel_event.scancode);
    Core::EventLoop::current().post_event(WindowManager::the(), move(message));
}

void Screen::constrain_pending_flush_rects()
{
    auto& flush_rects = *m_flush_rects;
    if (flush_rects.pending_flush_rects.is_empty())
        return;
    Gfx::IntRect screen_rect({}, rect().size());
    Gfx::DisjointIntRectSet rects;
    for (auto& fb_rect : flush_rects.pending_flush_rects) {
        Gfx::IntRect rect { (int)fb_rect.x, (int)fb_rect.y, (int)fb_rect.width, (int)fb_rect.height };
        auto intersected_rect = rect.intersected(screen_rect);
        if (!intersected_rect.is_empty())
            rects.add(intersected_rect);
    }
    flush_rects.pending_flush_rects.clear_with_capacity();
    for (auto const& rect : rects.rects()) {
        flush_rects.pending_flush_rects.append({
            .head_index = 0,
            .x = (unsigned)rect.x(),
            .y = (unsigned)rect.y(),
            .width = (unsigned)rect.width(),
            .height = (unsigned)rect.height(),
        });
    }
}

void Screen::queue_flush_display_rect(Gfx::IntRect const& flush_region)
{
    // NOTE: we don't scale until in Screen::flush_display so that when
    // there are too many rectangles that we end up throwing away, we didn't
    // waste accounting for scale factor!
    auto& flush_rects = *m_flush_rects;
    if (flush_rects.too_many_pending_flush_rects) {
        // We already have too many, just make sure we extend it if needed
        VERIFY(!flush_rects.pending_flush_rects.is_empty());
        if (flush_rects.pending_flush_rects.size() == 1) {
            auto& union_rect = flush_rects.pending_flush_rects[0];
            auto new_union = flush_region.united(Gfx::IntRect((int)union_rect.x, (int)union_rect.y, (int)union_rect.width, (int)union_rect.height));
            union_rect.x = new_union.left();
            union_rect.y = new_union.top();
            union_rect.width = new_union.width();
            union_rect.height = new_union.height();
        } else {
            // Convert all the rectangles into one union
            auto new_union = flush_region;
            for (auto& flush_rect : flush_rects.pending_flush_rects)
                new_union = new_union.united(Gfx::IntRect((int)flush_rect.x, (int)flush_rect.y, (int)flush_rect.width, (int)flush_rect.height));
            flush_rects.pending_flush_rects.resize(1, true);
            auto& union_rect = flush_rects.pending_flush_rects[0];
            union_rect.x = new_union.left();
            union_rect.y = new_union.top();
            union_rect.width = new_union.width();
            union_rect.height = new_union.height();
        }
        return;
    }
    VERIFY(flush_rects.pending_flush_rects.size() < flush_rects.pending_flush_rects.capacity());
    flush_rects.pending_flush_rects.append({ 0,
        (unsigned)flush_region.left(),
        (unsigned)flush_region.top(),
        (unsigned)flush_region.width(),
        (unsigned)flush_region.height() });
    if (flush_rects.pending_flush_rects.size() == flush_rects.pending_flush_rects.capacity()) {
        // If we get one more rectangle then we need to convert it to a single union rectangle
        flush_rects.too_many_pending_flush_rects = true;
    }
}

void Screen::flush_display(int buffer_index)
{
    VERIFY(m_backend->m_can_device_flush_buffers || m_backend->m_can_device_flush_entire_framebuffer);
    auto& flush_rects = *m_flush_rects;
    if (flush_rects.pending_flush_rects.is_empty())
        return;

    // Now that we have a final set of rects, apply the scale factor
    auto scale_factor = this->scale_factor();
    for (auto& flush_rect : flush_rects.pending_flush_rects) {
        VERIFY(Gfx::IntRect({}, m_virtual_rect.size()).contains({ (int)flush_rect.x, (int)flush_rect.y, (int)flush_rect.width, (int)flush_rect.height }));
        flush_rect.x *= scale_factor;
        flush_rect.y *= scale_factor;
        flush_rect.width *= scale_factor;
        flush_rect.height *= scale_factor;
    }

    if (m_backend->m_can_device_flush_entire_framebuffer) {
        auto return_value = m_backend->flush_framebuffer();
        if (return_value.is_error())
            dbgln("Screen #{}: Error flushing display: {}", index(), return_value.error());
    } else {
        auto return_value = m_backend->flush_framebuffer_rects(buffer_index, flush_rects.pending_flush_rects.span());
        if (return_value.is_error())
            dbgln("Screen #{}: Error flushing display: {}", index(), return_value.error());
    }

    flush_rects.too_many_pending_flush_rects = false;
    flush_rects.pending_flush_rects.clear_with_capacity();
}

void Screen::flush_display_entire_framebuffer()
{
    VERIFY(m_backend->m_can_device_flush_entire_framebuffer);
    auto return_value = m_backend->flush_framebuffer();
    if (return_value.is_error())
        dbgln("Screen #{}: Error flushing display front buffer: {}", index(), return_value.error());
}

void Screen::flush_display_front_buffer(int front_buffer_index, Gfx::IntRect& rect)
{
    VERIFY(m_backend->m_can_device_flush_buffers);
    auto scale_factor = this->scale_factor();
    FBRect flush_rect {
        .head_index = 0,
        .x = (unsigned)(rect.x() * scale_factor),
        .y = (unsigned)(rect.y() * scale_factor),
        .width = (unsigned)(rect.width() * scale_factor),
        .height = (unsigned)(rect.height() * scale_factor)
    };

    VERIFY(Gfx::IntRect({}, m_virtual_rect.size()).contains(rect));

    auto return_value = m_backend->flush_framebuffer_rects(front_buffer_index, { &flush_rect, 1 });
    if (return_value.is_error())
        dbgln("Screen #{}: Error flushing display front buffer: {}", index(), return_value.error());
}

}
