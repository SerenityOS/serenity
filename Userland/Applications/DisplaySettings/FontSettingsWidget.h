/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace DisplaySettings {

class FontSettingsWidget : public GUI::Widget {
    C_OBJECT(FontSettingsWidget);

public:
    virtual ~FontSettingsWidget() override;

    void apply_settings();

private:
    FontSettingsWidget();
};

}
