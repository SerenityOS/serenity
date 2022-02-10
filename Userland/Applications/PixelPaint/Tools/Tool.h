/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <LibGUI/Action.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/StandardCursor.h>

namespace PixelPaint {

class ImageEditor;
class Layer;

class Tool {
public:
    virtual ~Tool() = default;

    class MouseEvent {
    public:
        enum class Action {
            MouseDown,
            MouseMove,
            MouseUp
        };

        MouseEvent(Action action, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event, GUI::MouseEvent& raw_event)
            : m_action(action)
            , m_layer_event(layer_event)
            , m_image_event(image_event)
            , m_raw_event(raw_event)
        {
        }

        Action action() const { return m_action; }
        GUI::MouseEvent const& layer_event() const { return m_layer_event; }
        GUI::MouseEvent const& image_event() const { return m_image_event; }
        GUI::MouseEvent const& raw_event() const { return m_raw_event; }

    private:
        Action m_action;

        GUI::MouseEvent& m_layer_event;
        GUI::MouseEvent& m_image_event;
        GUI::MouseEvent& m_raw_event;
    };

    virtual void on_mousedown(Layer*, MouseEvent&) { }
    virtual void on_mousemove(Layer*, MouseEvent&) { }
    virtual void on_mouseup(Layer*, MouseEvent&) { }
    virtual void on_context_menu(Layer*, GUI::ContextMenuEvent&) { }
    virtual void on_tool_button_contextmenu(GUI::ContextMenuEvent&) { }
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) { }
    virtual void on_keydown(GUI::KeyEvent&);
    virtual void on_keyup(GUI::KeyEvent&) { }
    virtual void on_tool_activation() { }
    virtual GUI::Widget* get_properties_widget() { return nullptr; }
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() { return Gfx::StandardCursor::None; }

    void clear() { m_editor = nullptr; }
    void setup(ImageEditor&);

    ImageEditor const* editor() const { return m_editor; }
    ImageEditor* editor() { return m_editor; }

    GUI::Action* action() { return m_action; }
    void set_action(GUI::Action*);

protected:
    Tool() = default;
    WeakPtr<ImageEditor> m_editor;
    RefPtr<GUI::Action> m_action;

    virtual Gfx::IntPoint editor_stroke_position(Gfx::IntPoint const& pixel_coords, int stroke_thickness) const;

    void set_primary_slider(GUI::ValueSlider* primary) { m_primary_slider = primary; }
    void set_secondary_slider(GUI::ValueSlider* secondary) { m_secondary_slider = secondary; }

    GUI::ValueSlider* m_primary_slider { nullptr };
    GUI::ValueSlider* m_secondary_slider { nullptr };
};

}
