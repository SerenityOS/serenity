/*
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibCore/Timer.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/TextPosition.h>

namespace PixelPaint {
class TextTool;

class TextToolEditor : public GUI::TextEditor {
    C_OBJECT(TextToolEditor)
public:
    virtual ~TextToolEditor() override = default;
    virtual void handle_keyevent(Badge<TextTool>, GUI::KeyEvent&);
    Vector<NonnullRefPtr<GUI::Action>> actions();

protected:
    TextToolEditor();
};

class TextTool final : public Tool {
public:
    TextTool();
    virtual ~TextTool() override = default;

    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual void on_primary_color_change(Color) override;
    virtual void on_tool_deactivation() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;

private:
    virtual StringView tool_name() const override { return "Text Tool"sv; }
    void apply_text_to_layer();
    void reset_tool();

    RefPtr<GUI::Widget> m_properties_widget;
    RefPtr<GUI::Label> m_font_label;
    RefPtr<Core::Timer> m_cursor_blink_timer;
    RefPtr<PixelPaint::TextToolEditor> m_text_editor;
    Gfx::IntPoint m_add_text_position { 0, 0 };
    RefPtr<Gfx::Font const> m_selected_font;
    bool m_text_input_is_active { false };
    bool m_cursor_blink_state { false };
    bool m_mouse_is_over_text { false };
    bool m_is_dragging { false };
    Gfx::IntPoint m_drag_start_point;
    Gfx::IntRect m_ants_rect;
    Color m_text_color;
};

}
