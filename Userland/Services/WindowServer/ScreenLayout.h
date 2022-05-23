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
        enum class Mode {
            Invalid,
            Device,
            Virtual,
        } mode;
        Optional<String> device;
        Gfx::IntPoint location;
        Gfx::IntSize resolution;
        int scale_factor;

        Gfx::IntRect virtual_rect() const
        {
            return { location, { resolution.width() / scale_factor, resolution.height() / scale_factor } };
        }

        static StringView mode_to_string(Mode mode)
        {
#define __ENUMERATE_MODE_ENUM(val) \
    case Mode::val:                \
        return #val;

            switch (mode) {
                __ENUMERATE_MODE_ENUM(Invalid)
                __ENUMERATE_MODE_ENUM(Device)
                __ENUMERATE_MODE_ENUM(Virtual)
            }
            VERIFY_NOT_REACHED();

#undef __ENUMERATE_MODE_ENUM
        }

        bool operator==(Screen const&) const = default;
    };

    Vector<Screen> screens;
    unsigned main_screen_index { 0 };

    bool is_valid(String* error_msg = nullptr) const;
    bool normalize();
    bool load_config(Core::ConfigFile const& config_file, String* error_msg = nullptr);
    bool save_config(Core::ConfigFile& config_file, bool sync = true) const;
    bool try_auto_add_display_connector(String const&);

    // TODO: spaceship operator
    bool operator!=(ScreenLayout const& other) const;
    bool operator==(ScreenLayout const& other) const
    {
        return !(*this != other);
    }
};

}

namespace IPC {

bool encode(Encoder&, WindowServer::ScreenLayout::Screen const&);
ErrorOr<void> decode(Decoder&, WindowServer::ScreenLayout::Screen&);
bool encode(Encoder&, WindowServer::ScreenLayout const&);
ErrorOr<void> decode(Decoder&, WindowServer::ScreenLayout&);

}
