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

#pragma once

#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>

namespace PixelPaint {

class ImageEditor;
class Layer;

class Tool {
public:
    virtual ~Tool();

    virtual const char* class_name() const = 0;

    virtual void on_mousedown(Layer&, GUI::MouseEvent&, GUI::MouseEvent&) { }
    virtual void on_mousemove(Layer&, GUI::MouseEvent&, GUI::MouseEvent&) { }
    virtual void on_mouseup(Layer&, GUI::MouseEvent&, GUI::MouseEvent&) { }
    virtual void on_context_menu(Layer&, GUI::ContextMenuEvent&) { }
    virtual void on_tool_button_contextmenu(GUI::ContextMenuEvent&) { }
    virtual void on_second_paint(const Layer&, GUI::PaintEvent&) { }
    virtual void on_keydown(GUI::KeyEvent&) { }
    virtual void on_keyup(GUI::KeyEvent&) { }

    virtual bool is_move_tool() const { return false; }

    void clear() { m_editor = nullptr; }
    void setup(ImageEditor&);

    GUI::Action* action() { return m_action; }
    void set_action(GUI::Action*);

protected:
    Tool();
    WeakPtr<ImageEditor> m_editor;
    RefPtr<GUI::Action> m_action;
};

}
