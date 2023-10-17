/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/TextBox.h>

namespace Browser {

class URLBox : public GUI::TextBox {
    C_OBJECT(URLBox)

public:
    virtual ~URLBox() override = default;

    void set_focus_transition(bool focus_transition) { m_focus_transition = focus_transition; }
    bool is_focus_transition() const { return m_focus_transition; }

private:
    URLBox();

    void highlight_url();

    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void focusout_event(GUI::FocusEvent&) override;
    virtual void focusin_event(GUI::FocusEvent&) override;

    bool m_focus_transition { true };
};

}
