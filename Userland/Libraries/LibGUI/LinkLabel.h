/*
 * Copyright (c) 2020, Alex McGrath <amk@amk.ie>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/Label.h>

namespace GUI {

class LinkLabel : public Label {
    C_OBJECT(LinkLabel);

public:
    Function<void()> on_click;

private:
    explicit LinkLabel(String text = {});

    virtual void mousedown_event(MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;

    virtual void did_change_text() override;

    void update_tooltip_if_needed();
    void setup_actions();

    RefPtr<Menu> m_context_menu;
    RefPtr<Action> m_open_action;
    RefPtr<Action> m_copy_action;

    bool m_hovered { false };
};

}
