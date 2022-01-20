/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractButton.h>

namespace GUI {

class RadioButton : public AbstractButton {
    C_OBJECT(RadioButton)

public:
    virtual ~RadioButton() override;

    virtual void click(unsigned modifiers = 0) override;

protected:
    explicit RadioButton(String text = {});
    virtual void paint_event(PaintEvent&) override;

private:
    // These don't make sense for a radio button, so hide them.
    using AbstractButton::auto_repeat_interval;
    using AbstractButton::set_auto_repeat_interval;

    static Gfx::IntSize circle_size();
};

}
