/*
 * Copyright (c) 2023, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/SettingsWindow.h>
#include <LibGUI/SystemEffects.h>

namespace GUI {

namespace DisplaySettings {

class InterfaceSettingsWidget final : public SettingsWindow::Tab {
    C_OBJECT(InterfaceSettingsWidget);

public:
    virtual ~InterfaceSettingsWidget() override = default;

    virtual void apply_settings() override;

private:
    InterfaceSettingsWidget();

    void load_settings();
    RefPtr<GUI::CheckBox> m_global_menu;
};
}

}
