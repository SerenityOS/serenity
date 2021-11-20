/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Label.h>
#include <LibGUI/SettingsWindow.h>

namespace DisplaySettings {

class FontSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(FontSettingsWidget);

public:
    virtual ~FontSettingsWidget() override;

    virtual void apply_settings() override;

private:
    FontSettingsWidget();

    RefPtr<GUI::Label> m_default_font_label;
    RefPtr<GUI::Label> m_fixed_width_font_label;
};

}
