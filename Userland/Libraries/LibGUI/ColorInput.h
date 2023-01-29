/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/TextEditor.h>

namespace GUI {

class ColorInput final : public TextEditor {
    C_OBJECT(ColorInput);

public:
    virtual ~ColorInput() override = default;

    bool has_alpha_channel() const { return m_color_has_alpha_channel; }
    void set_color_has_alpha_channel(bool has_alpha) { m_color_has_alpha_channel = has_alpha; }

    void set_color(Color, AllowCallback = AllowCallback::Yes);
    Color color() { return m_color; }

    void set_color_picker_title(DeprecatedString title) { m_color_picker_title = move(title); }
    DeprecatedString color_picker_title() { return m_color_picker_title; }

    Function<void()> on_change;

protected:
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;

private:
    ColorInput();

    Gfx::IntRect color_rect() const;
    void set_color_internal(Color, AllowCallback, bool change_text);

    Color m_color;
    DeprecatedString m_color_picker_title { "Select color" };
    bool m_color_has_alpha_channel { true };
    bool m_may_be_color_rect_click { false };
};

}
