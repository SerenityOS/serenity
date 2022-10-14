/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/SettingsWindow.h>
#include <LibGUI/SystemEffects.h>

namespace GUI {

namespace DisplaySettings {

class EffectsSettingsWidget final : public SettingsWindow::Tab {
    C_OBJECT(EffectsSettingsWidget);

public:
    virtual ~EffectsSettingsWidget() override = default;

    virtual void apply_settings() override;

private:
    EffectsSettingsWidget();

    ErrorOr<void> load_settings();

    SystemEffects m_system_effects;
    Vector<DeprecatedString> m_geometry_list;
    RefPtr<ComboBox> m_geometry_combobox;
};

}

}
