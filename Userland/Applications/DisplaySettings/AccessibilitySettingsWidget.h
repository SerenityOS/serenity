/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/RadioButton.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/SettingsWindow.h>

namespace DisplaySettings {

class AccessibilitySettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(AccessibilitySettingsWidget);

public:
    virtual ~AccessibilitySettingsWidget() override = default;

    virtual void apply_settings() override;

private:
    AccessibilitySettingsWidget();

    RefPtr<GUI::RadioButton> m_filter_none;
    RefPtr<GUI::RadioButton> m_filter_protanopia;
    RefPtr<GUI::RadioButton> m_filter_protanomaly;
    RefPtr<GUI::RadioButton> m_filter_deuteranopia;
    RefPtr<GUI::RadioButton> m_filter_deuteranomaly;
    RefPtr<GUI::RadioButton> m_filter_tritanopia;
    RefPtr<GUI::RadioButton> m_filter_tritanomaly;
    RefPtr<GUI::RadioButton> m_filter_achromatopsia;
    RefPtr<GUI::RadioButton> m_filter_achromatomaly;

    RefPtr<GUI::ImageWidget> m_color_wheel;
};

}
