/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Common.h"
#include "Skin.h"
#include <LibGUI/Slider.h>

class AmpSlider final : public AutoSlider {
    C_OBJECT(AmpSlider)
public:
    enum class Type {
        Position,
        Volume,
        Balance,
    };

    virtual int knob_fixed_primary_size() const override { return m_knob_size; }
    virtual int knob_secondary_size() const override { return m_knob_size; }

protected:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;

private:
    explicit AmpSlider(const Skin&, Type);

    const Skin& m_skin;
    const Type m_type;

    Gfx::IntRect m_knob_rect;
    Gfx::IntRect m_knob_down_rect;
    int m_knob_size;
};
