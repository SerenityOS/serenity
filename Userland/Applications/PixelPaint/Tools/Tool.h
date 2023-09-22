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
            DoubleClick,
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

    virtual void on_doubleclick(Layer*, MouseEvent&) { }
    virtual void on_mousedown(Layer*, MouseEvent&) { }
    virtual void on_mousemove(Layer*, MouseEvent&) { }
    virtual void on_mouseup(Layer*, MouseEvent&) { }
    virtual void on_context_menu(Layer*, GUI::ContextMenuEvent&) { }
    virtual void on_tool_button_contextmenu(GUI::ContextMenuEvent&) { }
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) { }
    virtual bool on_keydown(GUI::KeyEvent&);
    virtual void on_keyup(GUI::KeyEvent&) { }
    virtual void on_primary_color_change(Color) { }
    virtual void on_secondary_color_change(Color) { }
    virtual void on_tool_activation() { }
    virtual void on_tool_deactivation() { }
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() { return GUI::Widget::construct(); }
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() { return Gfx::StandardCursor::None; }
    virtual Gfx::IntPoint point_position_to_preferred_cell(Gfx::FloatPoint position) const { return position.to_type<int>(); }

    void clear() { m_editor = nullptr; }
    void setup(ImageEditor&);

    ImageEditor const* editor() const { return m_editor; }
    ImageEditor* editor() { return m_editor; }

    GUI::Action* action() { return m_action; }
    void set_action(GUI::Action*);

    virtual StringView tool_name() const = 0;

    // We only set the override_alt_key flag to true since the override is false by default. If false is desired do not call method.
    virtual bool is_overriding_alt() { return false; }

protected:
    Tool() = default;
    WeakPtr<ImageEditor> m_editor;
    RefPtr<GUI::Action> m_action;

    Gfx::IntPoint editor_layer_location(Layer const& layer) const;

    virtual Gfx::IntPoint editor_stroke_position(Gfx::IntPoint pixel_coords, int stroke_thickness) const;

    void set_primary_slider(GUI::AbstractSlider* primary) { m_primary_slider = primary; }
    void set_secondary_slider(GUI::AbstractSlider* secondary) { m_secondary_slider = secondary; }

    static Gfx::IntPoint constrain_line_angle(Gfx::IntPoint start_pos, Gfx::IntPoint end_pos, float angle_increment = M_PI / 8);

    GUI::AbstractSlider* m_primary_slider { nullptr };
    GUI::AbstractSlider* m_secondary_slider { nullptr };

    template<Gfx::StorageFormat>
    void set_pixel_with_possible_mask(int x, int y, Gfx::Color color, Gfx::Bitmap& bitmap);
    void set_pixel_with_possible_mask(int x, int y, Gfx::Color color, Gfx::Bitmap& bitmap);
};

}
