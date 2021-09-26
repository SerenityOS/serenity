/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
#pragma once

#include "Skin.h"
#include <LibGUI/Button.h>

class AmpButton : public GUI::Button {
    C_OBJECT(AmpButton)

public:
    enum class Type {
        Previous,
        Play,
        Pause,
        Stop,
        Next,
        Eject,
        Minimize,
        Shade,
        Close,
        Window,
    };

protected:
    virtual void paint_event(GUI::PaintEvent&) override;

private:
    explicit AmpButton(const Skin&, Type);

    const Skin& m_skin;
    Type m_type;

    Gfx::IntRect m_rect;
    Gfx::IntRect m_down_rect;

    bool m_use_cbuttons;
};
