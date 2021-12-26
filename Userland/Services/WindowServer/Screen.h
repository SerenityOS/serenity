/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ScreenLayout.h"
#include <AK/NonnullOwnPtrVector.h>
#include <Kernel/API/KeyCode.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>

struct MousePacket;

namespace WindowServer {

constexpr double mouse_accel_max = 3.5;
constexpr double mouse_accel_min = 0.5;
constexpr unsigned scroll_step_size_min = 1;

// Most people will probably have 4 screens or less
constexpr size_t default_screen_count = 4;
// We currently only support 2 scale factors: 1x and 2x
constexpr size_t default_scale_factors_in_use_count = 2;

class Screen;

class ScreenInput {
public:
    static ScreenInput& the();

    Screen& cursor_location_screen();
    const Screen& cursor_location_screen() const;
    unsigned mouse_button_state() const { return m_mouse_button_state; }

    double acceleration_factor() const { return m_acceleration_factor; }
    void set_acceleration_factor(double);

    unsigned scroll_step_size() const { return m_scroll_step_size; }
    void set_scroll_step_size(unsigned);

    void on_receive_mouse_data(const MousePacket&);
    void on_receive_keyboard_data(::KeyEvent);

    Gfx::IntPoint cursor_location() const { return m_cursor_location; }
    void set_cursor_location(const Gfx::IntPoint point) { m_cursor_location = point; }

private:
    Gfx::IntPoint m_cursor_location;
    unsigned m_mouse_button_state { 0 };
    unsigned m_modifiers { 0 };
    double m_acceleration_factor { 1.0 };
    unsigned m_scroll_step_size { 1 };
};

class Screen {
public:
    template<typename... Args>
    static Screen* create(Args&&... args)
    {
        auto screen = adopt_own(*new Screen(forward<Args>(args)...));
        if (!screen->is_opened())
            return nullptr;
        auto* screen_ptr = screen.ptr();
        s_screens.append(move(screen));
        update_indices();
        update_bounding_rect();
        if (!s_main_screen)
            s_main_screen = screen_ptr;
        screen_ptr->init();
        return screen_ptr;
    }
    ~Screen();

    static bool apply_layout(ScreenLayout&&, String&);
    static const ScreenLayout& layout() { return s_layout; }

    static Screen& main()
    {
        VERIFY(s_main_screen);
        return *s_main_screen;
    }

    static Screen& closest_to_rect(const Gfx::IntRect&);
    static Screen& closest_to_location(const Gfx::IntPoint&);

    static Screen* find_by_index(size_t index)
    {
        if (index >= s_screens.size())
            return nullptr;
        return &s_screens[index];
    }

    static Vector<Gfx::IntRect, 4> rects()
    {
        Vector<Gfx::IntRect, 4> rects;
        for (auto& screen : s_screens)
            rects.append(screen.rect());
        return rects;
    }

    static Screen* find_by_location(const Gfx::IntPoint& point)
    {
        for (auto& screen : s_screens) {
            if (screen.rect().contains(point))
                return &screen;
        }
        return nullptr;
    }

    static const Gfx::IntRect& bounding_rect() { return s_bounding_screens_rect; }

    static size_t count() { return s_screens.size(); }
    size_t index() const { return m_index; }

    template<typename F>
    static IterationDecision for_each(F f)
    {
        for (auto& screen : s_screens) {
            IterationDecision decision = f(screen);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    template<typename F>
    static IterationDecision for_each_scale_factor_in_use(F f)
    {
        for (auto& scale_factor : s_scale_factors_in_use) {
            IterationDecision decision = f(scale_factor);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    void make_main_screen() { s_main_screen = this; }
    bool is_main_screen() const { return s_main_screen == this; }

    bool can_set_buffer() { return m_can_set_buffer; }
    void set_buffer(int index);

    int physical_width() const { return width() * scale_factor(); }
    int physical_height() const { return height() * scale_factor(); }
    size_t pitch() const { return m_pitch; }

    int width() const { return m_virtual_rect.width(); }
    int height() const { return m_virtual_rect.height(); }
    int scale_factor() const { return m_info.scale_factor; }

    Gfx::RGBA32* scanline(int y);

    Gfx::IntSize physical_size() const { return { physical_width(), physical_height() }; }

    Gfx::IntSize size() const { return { m_virtual_rect.width(), m_virtual_rect.height() }; }
    Gfx::IntRect rect() const { return m_virtual_rect; }

private:
    Screen(ScreenLayout::Screen&);
    bool open_device();
    void close_device();
    void init();
    bool set_resolution(bool initial);
    static void update_indices()
    {
        for (size_t i = 0; i < s_screens.size(); i++)
            s_screens[i].m_index = i;
    }
    static void update_bounding_rect();
    static void update_scale_factors_in_use();

    bool is_opened() const { return m_framebuffer_fd >= 0; }

    static NonnullOwnPtrVector<Screen, default_screen_count> s_screens;
    static Screen* s_main_screen;
    static Gfx::IntRect s_bounding_screens_rect;
    static ScreenLayout s_layout;
    static Vector<int, default_scale_factors_in_use_count> s_scale_factors_in_use;
    size_t m_index { 0 };

    size_t m_size_in_bytes;

    Gfx::RGBA32* m_framebuffer { nullptr };
    bool m_can_set_buffer { false };

    int m_pitch { 0 };
    Gfx::IntRect m_virtual_rect;
    int m_framebuffer_fd { -1 };

    ScreenLayout::Screen& m_info;
};

inline Gfx::RGBA32* Screen::scanline(int y)
{
    return reinterpret_cast<Gfx::RGBA32*>(((u8*)m_framebuffer) + (y * m_pitch));
}

}
