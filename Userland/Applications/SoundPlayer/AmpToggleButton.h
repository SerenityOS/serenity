/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Skin.h"
#include <LibGUI/CheckBox.h>

class AmpToggleButton final : public GUI::CheckBox {
    C_OBJECT(AmpToggleButton)

public:
    enum class Type {
        Equalizer,
        Playlist,
        Repeat,
        Shuffle,
    };

protected:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;

private:
    explicit AmpToggleButton(const Skin&, Type);

    const Skin& m_skin;
    Gfx::IntRect m_up_unchecked_rect;
    Gfx::IntRect m_up_checked_rect;
    Gfx::IntRect m_down_unchecked_rect;
    Gfx::IntRect m_down_checked_rect;
    bool m_mouse_down { false };
};
