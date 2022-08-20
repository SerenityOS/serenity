/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ColorInput.h>
#include <LibGUI/SettingsWindow.h>

class CardSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(CardSettingsWidget)
public:
    virtual ~CardSettingsWidget() override = default;

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    CardSettingsWidget();

    RefPtr<GUI::ColorInput> m_background_color_input;
};
