/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Services/WindowServer/ScreenLayout.h>

// Must be included after LibIPC/Forward.h
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace WindowServer {

bool ScreenLayout::is_valid(String* error_msg) const
{
    if (screens.is_empty()) {
        if (error_msg)
            *error_msg = "Must have at least one screen";
        return false;
    }
    if (main_screen_index >= screens.size()) {
        if (error_msg)
            *error_msg = String::formatted("Invalid main screen index: {}", main_screen_index);
        return false;
    }
    int smallest_x = 0;
    int smallest_y = 0;
    for (size_t i = 0; i < screens.size(); i++) {
        auto& screen = screens[i];
        if (screen.device.is_null() || screen.device.is_empty()) {
            if (error_msg)
                *error_msg = String::formatted("Screen #{} has no path", i);
            return false;
        }
        for (size_t j = 0; j < screens.size(); j++) {
            auto& other_screen = screens[j];
            if (&other_screen == &screen)
                continue;
            if (screen.device == other_screen.device) {
                if (error_msg)
                    *error_msg = String::formatted("Screen #{} is using same device as screen #{}", i, j);
                return false;
            }
            if (screen.virtual_rect().intersects(other_screen.virtual_rect())) {
                if (error_msg)
                    *error_msg = String::formatted("Screen #{} overlaps with screen #{}", i, j);
                return false;
            }
        }
        if (screen.location.x() < 0 || screen.location.y() < 0) {
            if (error_msg)
                *error_msg = String::formatted("Screen #{} has invalid location: {}", i, screen.location);
            return false;
        }
        if (screen.resolution.width() <= 0 || screen.resolution.height() <= 0) {
            if (error_msg)
                *error_msg = String::formatted("Screen #{} has invalid resolution: {}", i, screen.resolution);
            return false;
        }
        if (screen.scale_factor < 1) {
            if (error_msg)
                *error_msg = String::formatted("Screen #{} has invalid scale factor: {}", i, screen.scale_factor);
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
    Vector<const Screen*, 16> reachable_screens { &screens[main_screen_index] };
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
                    *error_msg = String::formatted("Screen #{} {} cannot be reached from main screen #{} {}", i, screen.virtual_rect(), main_screen_index, screens[main_screen_index].virtual_rect());
                break;
            }
        }
        return false;
    }
    return true;
}

void ScreenLayout::normalize()
{
    int smallest_x = 0;
    int smallest_y = 0;
    for (size_t i = 0; i < screens.size(); i++) {
        auto& screen = screens[i];
        if (i == 0 || screen.location.x() < smallest_x)
            smallest_x = screen.location.x();
        if (i == 0 || screen.location.y() < smallest_y)
            smallest_y = screen.location.y();
    }
    if (smallest_x != 0 || smallest_y != 0) {
        for (auto& screen : screens)
            screen.location.translate_by(-smallest_x, -smallest_y);
    }
}

bool ScreenLayout::load_config(const Core::ConfigFile& config_file, String* error_msg)
{
    screens.clear_with_capacity();
    main_screen_index = config_file.read_num_entry("Screens", "DefaultScreen", 0);
    for (size_t index = 0;; index++) {
        auto group_name = String::formatted("Screen{}", index);
        if (!config_file.has_group(group_name))
            break;
        screens.append({ config_file.read_entry(group_name, "Device"),
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
    config_file.write_num_entry("Screens", "DefaultScreen", main_screen_index);

    size_t index = 0;
    while (index < screens.size()) {
        auto& screen = screens[index];
        auto group_name = String::formatted("Screen{}", index);
        config_file.write_entry(group_name, "Device", screen.device);
        config_file.write_num_entry(group_name, "Left", screen.location.x());
        config_file.write_num_entry(group_name, "Top", screen.location.y());
        config_file.write_num_entry(group_name, "Width", screen.resolution.width());
        config_file.write_num_entry(group_name, "Height", screen.resolution.height());
        config_file.write_num_entry(group_name, "ScaleFactor", screen.scale_factor);
        index++;
    }
    // Prune screens no longer in the layout
    for (;;) {
        auto group_name = String::formatted("Screen{}", index++);
        if (!config_file.has_group(group_name))
            break;
        config_file.remove_group(group_name);
    }

    if (sync && !config_file.sync())
        return false;
    return true;
}

bool ScreenLayout::operator!=(const ScreenLayout& other) const
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

}

namespace IPC {

bool encode(Encoder& encoder, const WindowServer::ScreenLayout::Screen& screen)
{
    encoder << screen.device << screen.location << screen.resolution << screen.scale_factor;
    return true;
}

bool decode(Decoder& decoder, WindowServer::ScreenLayout::Screen& screen)
{
    String device;
    if (!decoder.decode(device))
        return false;
    Gfx::IntPoint location;
    if (!decoder.decode(location))
        return false;
    Gfx::IntSize resolution;
    if (!decoder.decode(resolution))
        return false;
    int scale_factor = 0;
    if (!decoder.decode(scale_factor))
        return false;
    screen = { device, location, resolution, scale_factor };
    return true;
}

bool encode(Encoder& encoder, const WindowServer::ScreenLayout& screen_layout)
{
    encoder << screen_layout.screens << screen_layout.main_screen_index;
    return true;
}

bool decode(Decoder& decoder, WindowServer::ScreenLayout& screen_layout)
{
    Vector<WindowServer::ScreenLayout::Screen> screens;
    if (!decoder.decode(screens))
        return false;
    unsigned main_screen_index = 0;
    if (!decoder.decode(main_screen_index))
        return false;
    screen_layout = { move(screens), main_screen_index };
    return true;
}

}
