/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/FileWatcher.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Window.h>

enum class ClearBackground {
    No,
    Yes
};

class KeymapStatusWidget : public GUI::Label {
    C_OBJECT(KeymapStatusWidget);

    virtual void mousedown_event(GUI::MouseEvent& event) override;

    void set_current_keymap(DeprecatedString const& keymap, ClearBackground clear_background = ClearBackground::Yes);

private:
    RefPtr<GUI::Menu> m_context_menu;
    DeprecatedString m_current_keymap;

    ErrorOr<void> refresh_menu();

    GUI::ActionGroup m_keymaps_group;
};
