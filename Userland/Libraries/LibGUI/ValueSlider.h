/*
 * Copyright (c) 2021, Marcus Nilsson <brainbomb@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGUI/AbstractSlider.h>

namespace GUI {

class ValueSlider : public AbstractSlider {
    C_OBJECT(ValueSlider);

public:
    enum class KnobStyle {
        Wide,
        Thin,
    };

    virtual ~ValueSlider() override = default;

    void set_suffix(String suffix) { m_suffix = move(suffix); }
    void set_knob_style(KnobStyle knobstyle) { m_knob_style = knobstyle; }

    virtual void set_value(int value, AllowCallback = AllowCallback::Yes, DoClamp = DoClamp::Yes) override;

protected:
    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;

private:
    explicit ValueSlider(Gfx::Orientation = Gfx::Orientation::Horizontal, String suffix = {});

    ByteString formatted_value() const;
    int value_at(Gfx::IntPoint position) const;
    Gfx::IntRect bar_rect() const;
    Gfx::IntRect knob_rect() const;
    int knob_length() const;

    virtual Optional<UISize> calculated_min_size() const override;
    virtual Optional<UISize> calculated_preferred_size() const override;

    String m_suffix {};
    Orientation m_orientation { Orientation::Horizontal };
    KnobStyle m_knob_style { KnobStyle::Thin };
    RefPtr<GUI::TextBox> m_textbox;
    bool m_dragging { false };
    bool m_hovered { false };
};

}
