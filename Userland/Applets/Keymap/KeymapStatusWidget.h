/*
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ActionGroup.h>
#include <LibGUI/Widget.h>

class KeymapStatusWidget final : public GUI::Widget {
    C_OBJECT(KeymapStatusWidget);

public:
    virtual ~KeymapStatusWidget() override;
    void set_current_keymap(ByteString const& keymap);

private:
    KeymapStatusWidget();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    ErrorOr<void> refresh_menu();

    RefPtr<GUI::Menu> m_context_menu;

    ByteString m_current_keymap;
    GUI::ActionGroup m_keymaps_group;
};
