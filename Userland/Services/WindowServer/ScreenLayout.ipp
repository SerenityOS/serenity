/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <sys/devices/gpu.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <Services/WindowServer/ScreenLayout.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>

namespace WindowServer {

bool ScreenLayout::is_valid(ByteString* error_msg) const
{
    if (screens.is_empty()) {
        if (error_msg)
            *error_msg = "Must have at least one screen";
        return false;
    }
    if (main_screen_index >= screens.size()) {
        if (error_msg)
            *error_msg = ByteString::formatted("Invalid main screen index: {}", main_screen_index);
        return false;
    }
    int smallest_x = 0;
    int smallest_y = 0;
    for (size_t i = 0; i < screens.size(); i++) {
        auto& screen = screens[i];
        if (screen.mode == Screen::Mode::Device && screen.device->is_empty()) {
            if (error_msg)
                *error_msg = ByteString::formatted("Screen #{} has no path", i);
            return false;
        }
        for (size_t j = 0; j < screens.size(); j++) {
            auto& other_screen = screens[j];
            if (&other_screen == &screen)
                continue;
            if (screen.device == other_screen.device) {
                if (error_msg)
                    *error_msg = ByteString::formatted("Screen #{} is using same device as screen #{}", i, j);
                return false;
            }
            if (screen.virtual_rect().intersects(other_screen.virtual_rect())) {
                if (error_msg)
                    *error_msg = ByteString::formatted("Screen #{} overlaps with screen #{}", i, j);
                return false;
            }
        }
        if (screen.location.x() < 0 || screen.location.y() < 0) {
            if (error_msg)
                *error_msg = ByteString::formatted("Screen #{} has invalid location: {}", i, screen.location);
            return false;
        }
        if (screen.resolution.width() <= 0 || screen.resolution.height() <= 0) {
            if (error_msg)
                *error_msg = ByteString::formatted("Screen #{} has invalid resolution: {}", i, screen.resolution);
            return false;
        }
        if (screen.scale_factor < 1) {
            if (error_msg)
                *error_msg = ByteString::formatted("Screen #{} has invalid scale factor: {}", i, screen.scale_factor);
            return false;
        }
        if (i == 0 || screen.location.x() < smallest_x)
            smallest_x = screen.location.x();
        if (i == 0 || screen.location.y() < smallest_y)
            smallest_y = screen.location.y();
    }
    if (smallest_x != 0 || smallest_y != 0) {
        if (error_msg)
            *error_msg = "Screen layout has not been normalized";
        return false;
    }
    Vector<Screen const*, 16> reachable_screens { &screens[main_screen_index] };
    bool did_reach_another_screen;
    do {
        did_reach_another_screen = false;
        auto* latest_reachable_screen = reachable_screens[reachable_screens.size() - 1];
        for (auto& screen : screens) {
            if (&screen == latest_reachable_screen || reachable_screens.contains_slow(&screen))
                continue;
            if (screen.virtual_rect().is_adjacent(latest_reachable_screen->virtual_rect())) {
                reachable_screens.append(&screen);
                did_reach_another_screen = true;
                break;
            }
        }
    } while (did_reach_another_screen);
    if (reachable_screens.size() != screens.size()) {
        for (size_t i = 0; i < screens.size(); i++) {
            auto& screen = screens[i];
            if (!reachable_screens.contains_slow(&screen)) {
                if (error_msg)
                    *error_msg = ByteString::formatted("Screen #{} {} cannot be reached from main screen #{} {}", i, screen.virtual_rect(), main_screen_index, screens[main_screen_index].virtual_rect());
                break;
            }
        }
        return false;
    }
    return true;
}

bool ScreenLayout::normalize()
{
    // Check for any overlaps and try to move screens
    Vector<Gfx::IntRect, 8> screen_virtual_rects;
    for (auto& screen : screens)
        screen_virtual_rects.append(screen.virtual_rect());

    bool did_change = false;
    for (;;) {
        // Separate any overlapping screens
        if (Gfx::IntRect::disperse(screen_virtual_rects)) {
            did_change = true;
            continue;
        }

        // Check if all screens are still reachable
        Vector<Gfx::IntRect*, 8> reachable_rects;

        auto recalculate_reachable = [&]() {
            reachable_rects = { &screen_virtual_rects[main_screen_index] };
            bool did_reach_another;
            do {
                did_reach_another = false;
                auto& latest_reachable_rect = *reachable_rects[reachable_rects.size() - 1];
                for (auto& rect : screen_virtual_rects) {
                    if (&rect == &latest_reachable_rect || reachable_rects.contains_slow(&rect))
                        continue;
                    if (rect.is_adjacent(latest_reachable_rect)) {
                        reachable_rects.append(&rect);
                        did_reach_another = true;
                        break;
                    }
                }
            } while (did_reach_another);
        };

        recalculate_reachable();
        if (reachable_rects.size() != screen_virtual_rects.size()) {
            // Some screens were not reachable, try to move one somewhere closer
            for (auto& screen_rect : screen_virtual_rects) {
                if (reachable_rects.contains_slow(&screen_rect))
                    continue;

                float closest_distance = 0;
                Gfx::IntRect* closest_rect = nullptr;
                for (auto& screen_rect2 : screen_virtual_rects) {
                    if (&screen_rect2 == &screen_rect)
                        continue;
                    if (!reachable_rects.contains_slow(&screen_rect2))
                        continue;
                    auto distance = screen_rect.outside_center_point_distance_to(screen_rect2);
                    if (!closest_rect || distance < closest_distance) {
                        closest_distance = distance;
                        closest_rect = &screen_rect2;
                    }
                }
                VERIFY(closest_rect); // We should always have one!
                VERIFY(closest_rect != &screen_rect);

                // Move the screen_rect closer to closest_rect
                auto is_adjacent_to_reachable = [&]() {
                    for (auto* rect : reachable_rects) {
                        if (rect == &screen_rect)
                            continue;
                        if (screen_rect.is_adjacent(*rect))
                            return true;
                    }
                    return false;
                };

                // Move it until we're touching a reachable screen
                do {
                    auto outside_center_points = screen_rect.closest_outside_center_points(*closest_rect);
                    int delta_x = 0;
                    if (outside_center_points[0].x() < outside_center_points[1].x())
                        delta_x = 1;
                    else if (outside_center_points[0].x() > outside_center_points[1].x())
                        delta_x = -1;
                    int delta_y = 0;
                    if (outside_center_points[0].y() < outside_center_points[1].y())
                        delta_y = 1;
                    else if (outside_center_points[0].y() > outside_center_points[1].y())
                        delta_y = -1;
                    VERIFY(delta_x != 0 || delta_y != 0);
                    screen_rect.translate_by(delta_x, delta_y);
                } while (!is_adjacent_to_reachable());

                recalculate_reachable();
                did_change = true;
                break; // We only try to move one at at time
            }

            // Moved the screen, re-evaluate
            continue;
        }
        break;
    }

    int smallest_x = 0;
    int smallest_y = 0;
    for (size_t i = 0; i < screen_virtual_rects.size(); i++) {
        auto& rect = screen_virtual_rects[i];
        if (i == 0 || rect.x() < smallest_x)
            smallest_x = rect.x();
        if (i == 0 || rect.y() < smallest_y)
            smallest_y = rect.y();
    }
    if (smallest_x != 0 || smallest_y != 0) {
        for (auto& rect : screen_virtual_rects)
            rect.translate_by(-smallest_x, -smallest_y);
        did_change = true;
    }

    for (size_t i = 0; i < screens.size(); i++)
        screens[i].location = screen_virtual_rects[i].location();

    VERIFY(is_valid());
    return did_change;
}

bool ScreenLayout::load_config(Core::ConfigFile const& config_file, ByteString* error_msg)
{
    screens.clear_with_capacity();
    main_screen_index = config_file.read_num_entry("Screens", "MainScreen", 0);
    for (size_t index = 0;; index++) {
        auto group_name = ByteString::formatted("Screen{}", index);
        if (!config_file.has_group(group_name))
            break;
        auto str_mode = config_file.read_entry(group_name, "Mode");
        Screen::Mode mode { Screen::Mode::Invalid };
        if (str_mode == "Device") {
            mode = Screen::Mode::Device;
        } else if (str_mode == "Virtual") {
            mode = Screen::Mode::Virtual;
        }

        if (mode == Screen::Mode::Invalid) {
            *error_msg = ByteString::formatted("Invalid screen mode '{}'", str_mode);
            *this = {};
            return false;
        }
        auto device = (mode == Screen::Mode::Device) ? config_file.read_entry(group_name, "Device") : Optional<ByteString> {};
        screens.append({ mode, device,
            { config_file.read_num_entry(group_name, "Left"), config_file.read_num_entry(group_name, "Top") },
            { config_file.read_num_entry(group_name, "Width"), config_file.read_num_entry(group_name, "Height") },
            config_file.read_num_entry(group_name, "ScaleFactor", 1) });
    }
    if (!is_valid(error_msg)) {
        *this = {};
        return false;
    }
    return true;
}

bool ScreenLayout::save_config(Core::ConfigFile& config_file, bool sync) const
{
    config_file.write_num_entry("Screens", "MainScreen", main_screen_index);

    size_t index = 0;
    while (index < screens.size()) {
        auto& screen = screens[index];
        auto group_name = ByteString::formatted("Screen{}", index);
        config_file.write_entry(group_name, "Mode", Screen::mode_to_string(screen.mode));
        if (screen.mode == Screen::Mode::Device)
            config_file.write_entry(group_name, "Device", screen.device.value());
        config_file.write_num_entry(group_name, "Left", screen.location.x());
        config_file.write_num_entry(group_name, "Top", screen.location.y());
        config_file.write_num_entry(group_name, "Width", screen.resolution.width());
        config_file.write_num_entry(group_name, "Height", screen.resolution.height());
        config_file.write_num_entry(group_name, "ScaleFactor", screen.scale_factor);
        index++;
    }
    // Prune screens no longer in the layout
    for (;;) {
        auto group_name = ByteString::formatted("Screen{}", index++);
        if (!config_file.has_group(group_name))
            break;
        config_file.remove_group(group_name);
    }

    if (sync && config_file.sync().is_error())
        return false;
    return true;
}

bool ScreenLayout::operator!=(ScreenLayout const& other) const
{
    if (this == &other)
        return false;
    if (main_screen_index != other.main_screen_index)
        return true;
    if (screens.size() != other.screens.size())
        return true;
    for (size_t i = 0; i < screens.size(); i++) {
        if (screens[i] != other.screens[i])
            return true;
    }
    return false;
}

bool ScreenLayout::try_auto_add_display_connector(ByteString const& device_path)
{
    int display_connector_fd = open(device_path.characters(), O_RDWR | O_CLOEXEC);
    if (display_connector_fd < 0) {
        int err = errno;
        dbgln("Error ({}) opening display connector device {}", err, device_path);
        return false;
    }
    ScopeGuard fd_guard([&] {
        close(display_connector_fd);
    });

    GraphicsHeadModeSetting mode_setting {};
    memset(&mode_setting, 0, sizeof(GraphicsHeadModeSetting));
    if (graphics_connector_get_head_mode_setting(display_connector_fd, &mode_setting) < 0) {
        int err = errno;
        dbgln("Error ({}) querying resolution from display connector device {}", err, device_path);
        return false;
    }
    if (mode_setting.horizontal_active == 0 || mode_setting.vertical_active == 0) {
        // Looks like the display is not turned on. Since we don't know what the desired
        // resolution should be, use the main display as reference.
        if (screens.is_empty())
            return false;
        auto& main_screen = screens[main_screen_index];
        mode_setting.horizontal_active = main_screen.resolution.width();
        mode_setting.vertical_active = main_screen.resolution.height();
    }

    auto append_screen = [&](Gfx::IntRect const& new_screen_rect) {
        screens.append({ .mode = Screen::Mode::Device,
            .device = device_path,
            .location = new_screen_rect.location(),
            .resolution = new_screen_rect.size(),
            .scale_factor = 1 });
    };

    if (screens.is_empty()) {
        append_screen({ 0, 0, mode_setting.horizontal_active, mode_setting.vertical_active });
        return true;
    }

    auto original_screens = move(screens);
    screens = original_screens;
    ArmedScopeGuard screens_guard([&] {
        screens = move(original_screens);
    });

    // Now that we know the current resolution, try to find a location that we can add onto
    // TODO: make this a little more sophisticated in case a more complex layout is already configured
    for (auto& screen : screens) {
        auto screen_rect = screen.virtual_rect();
        Gfx::IntRect new_screen_rect {
            screen_rect.right(),
            screen_rect.top(),
            (int)mode_setting.horizontal_active,
            (int)mode_setting.vertical_active
        };

        bool collision = false;
        for (auto& other_screen : screens) {
            if (&screen == &other_screen)
                continue;
            if (other_screen.virtual_rect().intersects(new_screen_rect)) {
                collision = true;
                break;
            }
        }

        if (!collision) {
            append_screen(new_screen_rect);
            if (is_valid()) {
                // We got lucky!
                screens_guard.disarm();
                return true;
            }
        }
    }

    dbgln("Failed to add display connector device {} with resolution {}x{} to screen layout", device_path, mode_setting.horizontal_active, mode_setting.vertical_active);
    return false;
}

}

namespace IPC {

template<>
ErrorOr<void> encode(Encoder& encoder, WindowServer::ScreenLayout::Screen const& screen)
{
    TRY(encoder.encode(screen.mode));
    TRY(encoder.encode(screen.device));
    TRY(encoder.encode(screen.location));
    TRY(encoder.encode(screen.resolution));
    TRY(encoder.encode(screen.scale_factor));

    return {};
}

template<>
ErrorOr<WindowServer::ScreenLayout::Screen> decode(Decoder& decoder)
{
    auto mode = TRY(decoder.decode<WindowServer::ScreenLayout::Screen::Mode>());
    auto device = TRY(decoder.decode<Optional<ByteString>>());
    auto location = TRY(decoder.decode<Gfx::IntPoint>());
    auto resolution = TRY(decoder.decode<Gfx::IntSize>());
    auto scale_factor = TRY(decoder.decode<int>());

    return WindowServer::ScreenLayout::Screen { mode, device, location, resolution, scale_factor };
}

template<>
ErrorOr<void> encode(Encoder& encoder, WindowServer::ScreenLayout const& screen_layout)
{
    TRY(encoder.encode(screen_layout.screens));
    TRY(encoder.encode(screen_layout.main_screen_index));

    return {};
}

template<>
ErrorOr<WindowServer::ScreenLayout> decode(Decoder& decoder)
{
    auto screens = TRY(decoder.decode<Vector<WindowServer::ScreenLayout::Screen>>());
    auto main_screen_index = TRY(decoder.decode<unsigned>());

    return WindowServer::ScreenLayout { move(screens), main_screen_index };
}

}
