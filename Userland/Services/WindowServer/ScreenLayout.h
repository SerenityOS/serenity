/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ConfigFile.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibIPC/Forward.h>

namespace WindowServer {

class ScreenLayout {
public:
    struct Screen {
        String device;
        Gfx::IntPoint location;
        Gfx::IntSize resolution;
        int scale_factor;

        Gfx::IntRect virtual_rect() const
        {
            return { location, { resolution.width() / scale_factor, resolution.height() / scale_factor } };
        }

        bool operator==(const Screen&) const = default;
    };

    Vector<Screen> screens;
    unsigned main_screen_index { 0 };

    bool is_valid(String* error_msg = nullptr) const;
    bool normalize();
    bool load_config(const Core::ConfigFile& config_file, String* error_msg = nullptr);
    bool save_config(Core::ConfigFile& config_file, bool sync = true) const;
    bool try_auto_add_framebuffer(String const&);

    // TODO: spaceship operator
    bool operator!=(const ScreenLayout& other) const;
    bool operator==(const ScreenLayout& other) const
    {
        return !(*this != other);
    }
};

}

namespace IPC {

bool encode(Encoder&, const WindowServer::ScreenLayout::Screen&);
ErrorOr<void> decode(Decoder&, WindowServer::ScreenLayout::Screen&);
bool encode(Encoder&, const WindowServer::ScreenLayout&);
ErrorOr<void> decode(Decoder&, WindowServer::ScreenLayout&);

}
