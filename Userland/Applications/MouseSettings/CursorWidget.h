/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/SettingsWindow.h>
#include <LibGUI/Window.h>

class CursorWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(CursorWidget)
public:
    virtual ~CursorWidget() override = default;

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    CursorWidget();

    RefPtr<GUI::HorizontalSlider> m_speed_slider;
    RefPtr<GUI::Label> m_speed_label;
    RefPtr<GUI::HorizontalSlider> m_size_slider;
    RefPtr<GUI::Label> m_size_label;
};
