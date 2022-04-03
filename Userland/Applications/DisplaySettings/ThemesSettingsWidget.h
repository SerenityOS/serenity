/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGfx/SystemTheme.h>

#include "ThemePreviewWidget.h"

namespace DisplaySettings {

class ThemesSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(ThemesSettingsWidget);

public:
    virtual void apply_settings() override;

private:
    Vector<Gfx::SystemThemeMetaData> m_themes;
    Vector<String> m_theme_names;

    RefPtr<GUI::ComboBox> m_themes_combo;
    RefPtr<ThemePreviewWidget> m_theme_preview;

    Gfx::SystemThemeMetaData const* m_selected_theme { nullptr };

    bool& m_background_settings_changed;

    ThemesSettingsWidget(bool& background_settings_changed);
};

}
