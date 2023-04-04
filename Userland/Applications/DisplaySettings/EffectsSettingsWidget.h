/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/SystemEffects.h>

namespace GUI {

namespace DisplaySettings {

class EffectsSettingsWidget final : public SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(EffectsSettingsWidget);

    static ErrorOr<NonnullRefPtr<EffectsSettingsWidget>> try_create();

public:
    virtual ~EffectsSettingsWidget() override = default;

    virtual void apply_settings() override;

private:
    EffectsSettingsWidget() = default;
    ErrorOr<void> setup_interface();

    ErrorOr<void> load_settings();

    SystemEffects m_system_effects;
    Vector<String> m_geometry_list;
    Vector<String> m_tile_window_list;
    RefPtr<ComboBox> m_geometry_combobox;
    RefPtr<ComboBox> m_tile_window_combobox;
};

}

}
