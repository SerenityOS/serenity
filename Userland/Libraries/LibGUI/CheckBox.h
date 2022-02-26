/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractButton.h>

namespace GUI {

class CheckBox : public AbstractButton {
    C_OBJECT(CheckBox);

public:
    virtual ~CheckBox() override = default;

    virtual void click(unsigned modifiers = 0) override;

    bool is_autosize() const { return m_autosize; }
    void set_autosize(bool);

private:
    explicit CheckBox(String = {});

    void size_to_fit();

    // These don't make sense for a check box, so hide them.
    using AbstractButton::auto_repeat_interval;
    using AbstractButton::set_auto_repeat_interval;

    virtual void paint_event(PaintEvent&) override;

    bool m_autosize { false };
};

}
