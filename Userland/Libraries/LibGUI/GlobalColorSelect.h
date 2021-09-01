/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

namespace GUI {

class GlobalColorSelect final : public Widget {
    C_OBJECT(GlobalColorSelect)
public:
    GlobalColorSelect();
    void begin_selecting();

    virtual ~GlobalColorSelect() override { }

    Function<void(Color const&)> on_color_changed;
    Function<void(Color const&)> on_finished;

private:
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    RefPtr<GUI::Window> m_window;
    Color m_col;
};

}