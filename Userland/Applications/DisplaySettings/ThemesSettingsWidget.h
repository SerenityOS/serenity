/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
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
    Vector<DeprecatedString> m_theme_names;
    Vector<DeprecatedString> m_color_scheme_names;

    RefPtr<GUI::ComboBox> m_themes_combo;
    RefPtr<ThemePreviewWidget> m_theme_preview;
    Gfx::SystemThemeMetaData const* m_selected_theme { nullptr };
    DeprecatedString m_selected_color_scheme_name = "";

    RefPtr<GUI::Button> m_cursor_themes_button;

    bool& m_background_settings_changed;
    bool m_color_scheme_is_file_based = true;

    ThemesSettingsWidget(bool& background_settings_changed);
};

}
