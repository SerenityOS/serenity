/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Application.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

class InputDebugWidget final : public GUI::Widget {
    C_OBJECT(InputDebugWidget);

private:
    virtual void keydown_event(GUI::KeyEvent& event) override
    {
        bool is_keypad = (event.modifiers() & Mod_Keypad) != 0;
        outln("KeyDown: key={}, code point={:#x}, is keypad?={}, scancode={:#x}, map entry index={:#x}",
            key_code_to_string(event.key()), event.code_point(), is_keypad, event.scancode(), event.map_entry_index());
    }

    virtual void keyup_event(GUI::KeyEvent& event) override
    {
        bool is_keypad = (event.modifiers() & Mod_Keypad) != 0;
        outln("KeyUp: key={}, code point={:#x}, is keypad?={}, scancode={:#x}, map entry index={:#x}",
            key_code_to_string(event.key()), event.code_point(), is_keypad, event.scancode(), event.map_entry_index());
    }

    virtual void mousemove_event(GUI::MouseEvent& event) override
    {
        outln("MouseMove: x={}, y={}", event.x(), event.y());
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        outln("MouseDown: button={}", GUI::mouse_button_to_string(event.button()));
    }

    virtual void mouseup_event(GUI::MouseEvent& event) override
    {
        outln("MouseUp: button={}", GUI::mouse_button_to_string(event.button()));
    }

    virtual void mousewheel_event(GUI::MouseEvent& event) override
    {
        outln("MouseWheel: dx={}, dy={}, raw dx={}, raw dy={}",
            event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y());
    }
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));
    auto window = TRY(GUI::Window::try_create());

    window->set_title("inputdbg");
    window->resize(200, 200);
    window->show();

    auto input_debug_widget = window->set_main_widget<InputDebugWidget>();

    return app->exec();
}
