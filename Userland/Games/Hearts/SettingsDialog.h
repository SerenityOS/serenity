/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibGUI/Dialog.h>

class SettingsDialog : public GUI::Dialog {
    C_OBJECT(SettingsDialog)
public:
    String const& player_name() const { return m_player_name; }

private:
    SettingsDialog(GUI::Window* parent, String player_name);

    String m_player_name { "Gunnar" };
};
