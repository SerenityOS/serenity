/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Badge.h>
#include <LibCore/ConfigFile.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Line.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/WindowServerConnection.h>
#include <string.h>
#include <unistd.h>

namespace GUI {

Desktop& Desktop::the()
{
    static Desktop* the;
    if (!the)
        the = new Desktop;
    return *the;
}

Desktop::Desktop()
{
}

void Desktop::did_receive_screen_rects(Badge<WindowServerConnection>, const Vector<Gfx::IntRect>& rects, u32 primary_screen)
{
    if (m_rects == rects && primary_screen == m_primary_screen)
        return;
    ASSERT(primary_screen < rects.size());
    m_rects = rects;
    m_primary_screen = primary_screen;
    if (on_rects_change)
        on_rects_change(rects, primary_screen);
}

void Desktop::set_background_color(const StringView& background_color)
{
    WindowServerConnection::the().post_message(Messages::WindowServer::SetBackgroundColor(background_color));
}

void Desktop::set_wallpaper_mode(const StringView& mode)
{
    WindowServerConnection::the().post_message(Messages::WindowServer::SetWallpaperMode(mode));
}

bool Desktop::set_wallpaper(const StringView& path, bool save_config)
{
    WindowServerConnection::the().post_message(Messages::WindowServer::AsyncSetWallpaper(path));
    auto ret_val = WindowServerConnection::the().wait_for_specific_message<Messages::WindowClient::AsyncSetWallpaperFinished>()->success();

    if (ret_val && save_config) {
        RefPtr<Core::ConfigFile> config = Core::ConfigFile::get_for_app("WindowManager");
        dbgln("Saving wallpaper path '{}' to config file at {}", path, config->file_name());
        config->write_entry("Background", "Wallpaper", path);
        config->sync();
    }

    return ret_val;
}

String Desktop::wallpaper() const
{
    return WindowServerConnection::the().send_sync<Messages::WindowServer::GetWallpaper>()->path();
}

Gfx::IntRect Desktop::get_screen_rect(size_t screen_index) const
{
    auto screen_rect = m_rects[screen_index];
    if (screen_index == m_primary_screen) {
        // TODO: This assumes that the task bar and menu bar are only on the primary screen!
        auto taskbar_height = GUI::Desktop::the().taskbar_height();
        auto menubar_height = GUI::Desktop::the().menubar_height();
        return { screen_rect.left(), screen_rect.top() + menubar_height, screen_rect.width(), screen_rect.height() - taskbar_height - menubar_height };
    }
    return screen_rect;
};

int Desktop::calculate_screen_area(size_t screen_index, const Gfx::IntRect& r, bool ignore_location) const
{
    auto screen_rect = get_screen_rect(screen_index);
    auto intersected_rect = screen_rect.intersected(ignore_location ? Gfx::IntRect{ screen_rect.location(), r.size() } : r);
    if (intersected_rect.is_empty())
        return -1;
    return intersected_rect.width() * intersected_rect.height();
};

size_t Desktop::find_best_screen(const Gfx::IntRect& r, bool ignore_location) const
{
    Optional<size_t> best_screen_index;
    int best_screen_area = 0;
    for (size_t i = 0; i < m_rects.size(); i++) {
        auto area = calculate_screen_area(i, r, ignore_location);
        if (area < 0)
            continue;
        if (!best_screen_index.has_value() || area >= best_screen_area) {
            if (i == m_primary_screen && area == best_screen_area && best_screen_index.has_value() && best_screen_index.value() != m_primary_screen) {
                // If it's a tie involving the primary screen, prefer the primary screen
                best_screen_index = i;
            } else if (!best_screen_index.has_value() || area > best_screen_area) {
                best_screen_index = i;
                best_screen_area = area;
            }
        }
    }
    ASSERT(best_screen_index.has_value());
    return best_screen_index.value();
};

Gfx::IntRect Desktop::calculate_ideal_visible_rect(const Gfx::IntSize& size, const Gfx::IntRect& exclude_rect, const Vector<SideWithAlignment>& desired_relative_side, bool allow_resize) const
{
    auto calculate_rect = [&](const Gfx::IntRect& bounds, const Gfx::IntSize& rect_size, const Gfx::IntPoint& magnet, const Gfx::IntRect::RelativeLocation& relative_location, const SideWithAlignment& requested) {
        auto do_align = [&](Gfx::IntPoint align_at) {
            Gfx::IntPoint align_at{ rect_size.width() / 2, rect_size.height() / 2 };
            auto align = Gfx::TextAlignment::Center;
            bool apply_vertical_alignment = false, apply_horizontal_alignment = false;
            if (relative_location.anywhere_left() && relative_location.left())
                apply_vertical_alignment = true;
            else if (relative_location.anywhere_right() && relative_location.right())
                apply_vertical_alignment = true;
            if (relative_location.anywhere_above() && relative_location.top())
                apply_horizontal_alignment = true;
            else if (relative_location.anywhere_below() && relative_location.bottom())
                apply_horizontal_alignment = true;

            ASSERT(apply_vertical_alignment ^ apply_horizontal_alignment);
            if (apply_vertical_alignment) {
                switch (requested.alignment) {
                case RectAlignment::Center:
                    // already applied by default
                    break;
                case RectAlignment::LeftOrTop:
                    // align at top
                    rect_center.set_y(rect_size.height() - 1);
                    break;
                case RectAlignment::RightOrBottom:
                    // align at bottom
                    rect_center.set_y(0);
                    break;
                }
            } else if (apply_horizontal_alignment) {
                switch (requested.alignment) {
                case RectAlignment::Center:
                    // already applied by default
                    break;
                case RectAlignment::LeftOrTop:
                    // align at left
                    rect_center.set_x(rect_size.width() - 1);
                    break;
                case RectAlignment::RightOrBottom:
                    // align at right
                    rect_center.set_x(0);
                    break;
                }
            }
        }

        if (bounds.is_adjacent(exclude_rect)) {
            // TODO: align directly along exclude_rect
        }

        auto rect = bounds.aligned_within(rect_size, magnet, align_at, );
        if (allow_resize)
            rect.intersect(bounds);
        return rect;
    };
    size_t best_screen_index = find_best_screen(exclude_rect, false);
    // Now that we know which screen is probably the best place, figure out
    // where we want to move it and what exact size we want for it.
    auto screen_rect = get_screen_rect(best_screen_index);
    auto parts = screen_rect.shatter(exclude_rect);
    if (parts.is_empty()) {
        // Not really any space left on this screen...
        // Find the closest adjacent screen and place it as close as possible to exclude_rect
        Optional<size_t> closest_index;
        Gfx::IntPoint closest_point_on_screen_rect{};
        float closest_distance = 0.0;
        for (size_t i = 0; i < m_rects.size(); i++) {
            if (i == best_screen_index)
                continue;
            auto rect = get_screen_rect(i);
            auto center_points = exclude_rect.closest_outside_center_points(rect);
            if (center_points.is_empty())
                continue;
            auto distance = Gfx::Line{ center_points[0], center_points[1] }.length();
            if (!closest_index.has_value() || distance < closest_distance) {
                closest_index = i;
                closest_point_on_screen_rect = center_points[1];
                closest_distance = distance;
            }
        }
        if (!closest_index.has_value()) {
            // We don't have any other screen and there is no area outside
            // of exclude_rect that we can use...
            return {};
        }
        auto closest_rect = get_screen_rect(closest_index.value());
        return calculate_rect(closest_rect, size, closest_point_on_screen_rect, {}, {});
    }

    auto fit_in_rect = [&](const Gfx::IntRect& rect, const SideWithAlignment& requested) -> Gfx::IntRect {
        auto center_points = exclude_rect.closest_outside_center_points(rect);
        if (center_points.is_empty()) {
            dbgln("no outside center points, rect={} to {}", rect, exclude_rect);
            return {};
        }
        return calculate_rect(rect, size, center_points[1], rect.relative_location_to(exclude_rect), requested);
    };

    auto get_rects_for_side = [&](Gfx::IntRect::Side side) {
        Gfx::DisjointRectSet rects_on_side;
        for (auto& r : parts) {
            if (auto rect = exclude_rect.rect_on_side(side, r); !rect.is_empty())
                rects_on_side.add(rect);
        }
        return rects_on_side;
    };
    auto request_for_side = [&](Gfx::IntRect::Side side) -> SideWithAlignment {
        for (auto& request : desired_relative_side) {
            if (request.side == Gfx::IntRect::Side::None)
                break;
            if (request.side == side)
                return request;
        }
        return { .side = side };
    };
    auto for_each_rects_in_side = [&]<typename F>(F f) {
        static const Gfx::IntRect::Side s_sides[] = {
            Gfx::IntRect::Side::Left, Gfx::IntRect::Side::Top, Gfx::IntRect::Side::Right, Gfx::IntRect::Side::Bottom
        };
        Vector<Gfx::IntRect, 16> rects;
        for (size_t i = 0; i < sizeof(s_sides) / sizeof(s_sides[0]); i++) {
            auto rects_for_side = get_rects_for_side(s_sides[i]);
            if (!rects_for_side.is_empty()) {
                IterationDecision decision = f(s_sides[i], rects_for_side.rects());
                if (decision != IterationDecision::Continue)
                    return decision;
            }
        }
        return IterationDecision::Continue;
    };

    if (desired_relative_side.is_empty()) {
        // If no specific side is requested, place it in the largest available area
        Optional<Gfx::IntRect> largest_rect;
        int largest_area = 0;
        auto largest_side = Gfx::IntRect::Side::None;
        for_each_rects_in_side([&](auto side, auto& rects) {
            for (auto& r : rects) {
                int area = r.width() * r.height();
                if (!largest_rect.has_value() || largest_area < area) {
                    largest_rect = r;
                    largest_area = area;
                    largest_side = side;
                }
            }
            return IterationDecision::Continue;
        });
        ASSERT(largest_rect.has_value());
        return fit_in_rect(largest_rect.value(), request_for_side(largest_side));
    }

    // Now try to find an area based on the list of preferred sides sorted by priority
    for (auto preferred : desired_relative_side) {
        if (preferred.side == Gfx::IntRect::Side::None) {
            // If we haven't found a match and we encounter Side::None
            // then bail, as it means that the caller does not want to
            // fall back to any available area other than at the sides
            // requested.
            dbgln("Have not found a suitable location and no fallback allowed!");
            return {};
        }

        auto rects_for_side = get_rects_for_side(preferred.side);
        for (auto& r : rects_for_side.rects()) {
            if (auto ideal_rect = fit_in_rect(r, preferred); !ideal_rect.is_empty())
                return ideal_rect;
        }
    }
    // We haven't found a match yet, and Side::None wasn't specified.
    // Try to fall back to anything that can fit
    Gfx::IntRect fallback_rect{};
    for_each_rects_in_side([&](auto side, auto& rects) {
        for (auto& r : rects) {
            if (auto ideal_rect = fit_in_rect(r, request_for_side(side)); !ideal_rect.is_empty()) {
                dbgln("fall back to fit in {} -> {}", r, ideal_rect);
                fallback_rect = ideal_rect;
                return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    });
    return fallback_rect;
}

}
