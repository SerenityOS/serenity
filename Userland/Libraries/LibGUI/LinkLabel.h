/*
 * Copyright (c) 2020, Alex McGrath <amk@amk.ie>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Label.h>

namespace GUI {

class LinkLabel : public Label {
    C_OBJECT_ABSTRACT(LinkLabel);

public:
    static ErrorOr<NonnullRefPtr<LinkLabel>> try_create(String text = {});

    Function<void()> on_click;

private:
    explicit LinkLabel(String text = {});

    ErrorOr<void> create_actions();
    void create_menus();

    virtual void mousemove_event(MouseEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;

    virtual void did_change_text() override;

    void update_tooltip_if_needed();
    void set_hovered(bool);

    RefPtr<Menu> m_context_menu;
    RefPtr<Action> m_open_action;
    RefPtr<Action> m_copy_action;

    bool m_hovered { false };
};

}
