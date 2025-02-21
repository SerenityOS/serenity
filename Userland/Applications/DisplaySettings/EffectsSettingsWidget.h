/*
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/SystemEffects.h>

namespace DisplaySettings {

class EffectsSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(EffectsSettingsWidget);

public:
    static ErrorOr<NonnullRefPtr<EffectsSettingsWidget>> try_create();
    virtual ~EffectsSettingsWidget() override = default;
    ErrorOr<void> initialize();

    virtual void apply_settings() override;

private:
    EffectsSettingsWidget() = default;
    ErrorOr<void> setup_interface();

    ErrorOr<void> load_settings();

    GUI::SystemEffects m_system_effects;
    Vector<String> m_geometry_list;
    Vector<String> m_tile_window_list;
    RefPtr<GUI::ComboBox> m_geometry_combobox;
    RefPtr<GUI::ComboBox> m_tile_window_combobox;
};

}
