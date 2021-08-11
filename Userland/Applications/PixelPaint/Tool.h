/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>
#include <LibGfx/StandardCursor.h>

namespace PixelPaint {

class ImageEditor;
class Layer;

class Tool {
public:
    virtual ~Tool();

    virtual void on_mousedown(Layer&, GUI::MouseEvent&, GUI::MouseEvent&) { }
    virtual void on_mousemove(Layer&, GUI::MouseEvent&, GUI::MouseEvent&) { }
    virtual void on_mouseup(Layer&, GUI::MouseEvent&, GUI::MouseEvent&) { }
    virtual void on_context_menu(Layer&, GUI::ContextMenuEvent&) { }
    virtual void on_tool_button_contextmenu(GUI::ContextMenuEvent&) { }
    virtual void on_second_paint(Layer const&, GUI::PaintEvent&) { }
    virtual void on_keydown(GUI::KeyEvent& event) { event.ignore(); }
    virtual void on_keyup(GUI::KeyEvent&) { }
    virtual void on_tool_activation() { }
    virtual GUI::Widget* get_properties_widget() { return nullptr; }
    virtual Gfx::StandardCursor cursor() { return Gfx::StandardCursor::None; }

    void clear() { m_editor = nullptr; }
    void setup(ImageEditor&);

    ImageEditor const* editor() const { return m_editor; }
    ImageEditor* editor() { return m_editor; }

    GUI::Action* action() { return m_action; }
    void set_action(GUI::Action*);

protected:
    Tool();
    WeakPtr<ImageEditor> m_editor;
    RefPtr<GUI::Action> m_action;
};

}
