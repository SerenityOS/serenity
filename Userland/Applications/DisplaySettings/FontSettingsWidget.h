/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Label.h>
#include <LibGUI/SettingsWindow.h>

namespace DisplaySettings {

class FontSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(FontSettingsWidget);

public:
    static ErrorOr<NonnullRefPtr<FontSettingsWidget>> try_create();
    virtual ~FontSettingsWidget() override = default;

    virtual void apply_settings() override;

private:
    FontSettingsWidget() = default;
    ErrorOr<void> setup_interface();

    RefPtr<GUI::Label> m_default_font_label;
    RefPtr<GUI::Label> m_window_title_font_label;
    RefPtr<GUI::Label> m_fixed_width_font_label;
};

}
