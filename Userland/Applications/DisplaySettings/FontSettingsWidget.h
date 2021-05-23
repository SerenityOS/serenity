/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>

namespace DisplaySettings {

class FontSettingsWidget : public GUI::Widget {
    C_OBJECT(FontSettingsWidget);

public:
    virtual ~FontSettingsWidget() override;

    void apply_settings();

private:
    FontSettingsWidget();

    RefPtr<GUI::Label> m_default_font_label;
    RefPtr<GUI::Label> m_fixed_width_font_label;
};

}
