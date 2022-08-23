/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Vector.h>
#include <Kernel/API/KeyCode.h>
#include <LibTextEditing/API.h>
#include <LibTextEditing/Forward.h>

namespace TextEditing {

// An engine performs text editing. It receives commands via an Editor.
class Engine {
public:
    Engine(Editor& editor);
    virtual ~Engine() = default;

    void send_key_down(KeyCode);
    void send_key_up(KeyCode);

    virtual void send_mouse_down(Position) = 0;
    virtual void send_mouse_move(Position) = 0;
    virtual void send_mouse_up(Position) = 0;

    virtual void set_viewport(Viewport, VerticalScrollBehavior) = 0;

private:
    NonnullRefPtr<Interface> interface();

    virtual void handle_key_change() = 0;

    Editor& m_editor;
    Vector<KeyCode, 3> m_pressed_keys;
};

}
