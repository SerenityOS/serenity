/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AppletManager.h"
#include "Compositor.h"
#include "EventLoop.h"
#include "Screen.h"
#include "WindowManager.h"
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <LibMain/Main.h>
#include <signal.h>
#include <sys/devices/gpu.h>

namespace WindowServer {
RefPtr<Core::ConfigFile> g_config;
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio video thread sendfd recvfd accept rpath wpath cpath unix proc getkeymap sigaction exec tty"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp", "cw"));
    TRY(Core::System::unveil("/etc/WindowServer.ini", "rwc"));
    TRY(Core::System::unveil("/etc/Keyboard.ini", "r"));
    TRY(Core::System::unveil("/dev/tty", "rw"));
    TRY(Core::System::unveil("/dev/gpu/", "rw"));
    TRY(Core::System::unveil("/dev/input/", "rw"));
    TRY(Core::System::unveil("/bin/keymap", "x"));
    TRY(Core::System::unveil("/sys/kernel/keymap", "r"));
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));

    struct sigaction act = {};
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    TRY(Core::System::sigaction(SIGCHLD, &act, nullptr));
    TRY(Core::System::pledge("stdio video thread sendfd recvfd accept rpath wpath cpath unix proc getkeymap exec tty"));

    WindowServer::g_config = TRY(Core::ConfigFile::open("/etc/WindowServer.ini", Core::ConfigFile::AllowWriting::Yes));
    auto theme_name = WindowServer::g_config->read_entry("Theme", "Name", "Default");

    Optional<ByteString> custom_color_scheme_path = OptionalNone();
    if (WindowServer::g_config->read_bool_entry("Theme", "LoadCustomColorScheme", false))
        custom_color_scheme_path = WindowServer::g_config->read_entry("Theme", "CustomColorSchemePath");

    auto theme = TRY(Gfx::load_system_theme(ByteString::formatted("/res/themes/{}.ini", theme_name), custom_color_scheme_path));
    Gfx::set_system_theme(theme);
    auto palette = Gfx::PaletteImpl::create_with_anonymous_buffer(theme);

    auto default_font_query = WindowServer::g_config->read_entry("Fonts", "Default", "Katica 10 400 0");
    auto fixed_width_font_query = WindowServer::g_config->read_entry("Fonts", "FixedWidth", "Csilla 10 400 0");
    auto window_title_font_query = WindowServer::g_config->read_entry("Fonts", "WindowTitle", "Katica 10 700 0");

    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);
    Gfx::FontDatabase::set_window_title_font_query(window_title_font_query);

    {
        // FIXME: Map switched tty from screens.
        // FIXME: Gracefully cleanup the TTY graphics mode.
        int tty_fd = TRY(Core::System::open("/dev/tty"sv, O_RDWR));
        TRY(Core::System::ioctl(tty_fd, KDSETMODE, KD_GRAPHICS));
        TRY(Core::System::close(tty_fd));
    }

    WindowServer::EventLoop loop;

    TRY(Core::System::pledge("stdio video thread sendfd recvfd accept rpath wpath cpath unix proc getkeymap exec"));

    // First check which screens are explicitly configured
    {
        AK::HashTable<ByteString> fb_devices_configured;
        WindowServer::ScreenLayout screen_layout;
        ByteString error_msg;

        auto add_unconfigured_display_connector_devices = [&]() -> ErrorOr<void> {
            // Enumerate the /dev/gpu/connectorX devices and try to set up any ones we find that we haven't already used
            Core::DirIterator di("/dev/gpu", Core::DirIterator::SkipParentAndBaseDir);
            while (di.has_next()) {
                auto path = di.next_path();
                if (!path.starts_with("connector"sv))
                    continue;
                auto full_path = ByteString::formatted("/dev/gpu/{}", path);
                if (!FileSystem::is_device(full_path))
                    continue;
                auto display_connector_fd = TRY(Core::System::open(full_path, O_RDWR | O_CLOEXEC));
                if (int rc = graphics_connector_set_responsible(display_connector_fd); rc != 0)
                    return Error::from_syscall("graphics_connector_set_responsible"sv, rc);
                TRY(Core::System::close(display_connector_fd));
                if (fb_devices_configured.find(full_path) != fb_devices_configured.end())
                    continue;
                if (!screen_layout.try_auto_add_display_connector(full_path))
                    dbgln("Could not auto-add display connector device {} to screen layout", full_path);
            }
            return {};
        };

        auto apply_and_generate_generic_screen_layout = [&]() -> ErrorOr<bool> {
            screen_layout = {};
            fb_devices_configured = {};

            TRY(add_unconfigured_display_connector_devices());
            if (!WindowServer::Screen::apply_layout(move(screen_layout), error_msg)) {
                dbgln("Failed to apply generated fallback screen layout: {}", error_msg);
                return false;
            }

            dbgln("Applied generated fallback screen layout!");
            return true;
        };

        if (screen_layout.load_config(*WindowServer::g_config, &error_msg)) {
            for (auto& screen_info : screen_layout.screens)
                if (screen_info.mode == WindowServer::ScreenLayout::Screen::Mode::Device)
                    fb_devices_configured.set(screen_info.device.value());

            TRY(add_unconfigured_display_connector_devices());

            if (!WindowServer::Screen::apply_layout(move(screen_layout), error_msg)) {
                dbgln("Error applying screen layout: {}", error_msg);
                TRY(apply_and_generate_generic_screen_layout());
            }
        } else {
            dbgln("Error loading screen configuration: {}", error_msg);
            TRY(apply_and_generate_generic_screen_layout());
        }
    }

    auto& screen_input = WindowServer::ScreenInput::the();
    screen_input.set_cursor_location(WindowServer::Screen::main().rect().center());
    double f = atof(WindowServer::g_config->read_entry("Mouse", "AccelerationFactor", "1.0").characters());
    if (f < WindowServer::mouse_accel_min || f > WindowServer::mouse_accel_max) {
        dbgln("Mouse.AccelerationFactor out of range resetting to 1.0");
        f = 1.0;
        WindowServer::g_config->write_entry("Mouse", "AccelerationFactor", "1.0");
    }
    screen_input.set_acceleration_factor(f);
    screen_input.set_scroll_step_size(WindowServer::g_config->read_num_entry("Mouse", "ScrollStepSize", 4));

    WindowServer::Compositor::the();
    auto wm = WindowServer::WindowManager::construct(*palette);
    auto am = WindowServer::AppletManager::construct();
    auto mm = WindowServer::MenuManager::construct();

    TRY(Core::System::unveil("/tmp", ""));

    TRY(Core::System::unveil(nullptr, nullptr));

    dbgln("Entering WindowServer main loop");
    loop.exec();
    VERIFY_NOT_REACHED();
}
