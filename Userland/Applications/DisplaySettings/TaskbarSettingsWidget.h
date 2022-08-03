/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGfx/SystemTheme.h>

#include "ThemePreviewWidget.h"

namespace DisplaySettings {

class TaskbarSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(TaskbarSettingsWidget);

public:
    virtual void apply_settings() override;

private:
    TaskbarSettingsWidget();

    RefPtr<GUI::RadioButton> m_classic;
    RefPtr<GUI::RadioButton> m_modern;
};

}
