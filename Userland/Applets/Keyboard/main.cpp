/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibGUI/Application.h>
#include <LibGUI/Label.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <serenity.h>
#include <sys/ioctl.h>

enum class KeyboardState {
    NUM_LOCK,
    CAPS_LOCK,
};

class KeyboardStateLabel final : public GUI::Label {
    C_OBJECT(KeyboardStateLabel)

public:
    KeyboardStateLabel(KeyboardState const keyboard_state, Optional<Gfx::Color> const& active_color, Optional<Gfx::Color> const& inactive_color)
        : m_keyboard_state(keyboard_state)
        , m_active_color(active_color.value_or(Color::from_rgb(0x00bb00)))
        , m_inactive_color(inactive_color.value_or(Color::from_rgb(0x000000)))
    {
        auto label_palette = palette();
        label_palette.set_color(Gfx::ColorRole::WindowText, m_inactive_color);
        set_palette(label_palette);
    }

    KeyboardState keyboard_state() const { return m_keyboard_state; }

    void set_state(bool active)
    {
        [[maybe_unused]] auto label_palette = palette();

        if (active && !m_active) {
            label_palette.set_color(Gfx::ColorRole::WindowText, m_active_color);
            set_palette(label_palette);
        } else if (!active && m_active) {
            label_palette.set_color(Gfx::ColorRole::WindowText, m_inactive_color);
            set_palette(label_palette);
        }

        m_active = active;
    }

private:
    KeyboardState m_keyboard_state;
    Gfx::Color m_active_color;
    Gfx::Color m_inactive_color;
    bool m_active = false;
};

int main(int argc, char* argv[])
{
    if (pledge("stdio recvfd sendfd rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* active_color = "#00cc00";
    const char* inactive_color = "#000000";
    Core::ArgsParser args_parser;
    args_parser.add_option(active_color, "Active color", "active-color", 'a', "active-color");
    args_parser.add_option(inactive_color, "Inactive color", "inactive-color", 'i', "inactive-color");
    args_parser.parse(argc, argv);

    if (unveil("/res/fonts", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/dev/keyboard0", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    NonnullRefPtrVector<GUI::Window> applet_windows;

    auto create_applet = [&](KeyboardState keyboard_state, StringView text) {
        auto window = GUI::Window::construct();
        window->set_window_type(GUI::WindowType::Applet);
        window->set_has_alpha_channel(true);

        auto& main_widget = window->set_main_widget<KeyboardStateLabel>(
            keyboard_state,
            Gfx::Color::from_string(active_color),
            Gfx::Color::from_string(inactive_color));
        main_widget.set_font_weight(Gfx::FontDatabase::default_font().bold_variant().weight());
        main_widget.set_text(text);
        main_widget.set_autosize(true);

        window->resize(main_widget.max_size().width() + 2, 16);
        window->show();

        applet_windows.append(move(window));
    };

    create_applet(KeyboardState::NUM_LOCK, "NUM"sv);
    create_applet(KeyboardState::CAPS_LOCK, "CAPS"sv);

    auto timer = Core::Timer::create_repeating(1000, [&] {
        auto keyboard_device_or_error = Core::File::open("/dev/keyboard0", Core::OpenMode::ReadOnly);
        if (keyboard_device_or_error.is_error()) {
            warnln("Failed to open /dev/keyboard0: {}", keyboard_device_or_error.error());
            VERIFY_NOT_REACHED();
        }
        auto keyboard_device = keyboard_device_or_error.release_value();

        bool num_lock_on;
        ioctl(keyboard_device->fd(), KEYBOARD_IOCTL_GET_NUM_LOCK, &num_lock_on);
        bool caps_lock_on;
        ioctl(keyboard_device->fd(), KEYBOARD_IOCTL_GET_CAPS_LOCK, &caps_lock_on);

        for (auto& applet : applet_windows) {
            auto keyboard_state_widget = (KeyboardStateLabel*)applet.main_widget();

            if (keyboard_state_widget->keyboard_state() == KeyboardState::NUM_LOCK)
                keyboard_state_widget->set_state(num_lock_on);
            if (keyboard_state_widget->keyboard_state() == KeyboardState::CAPS_LOCK)
                keyboard_state_widget->set_state(caps_lock_on);
        }

        keyboard_device->close();
    });
    timer->start();

    return app->exec();
}
