/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "HighlightPreviewWidget.h"
#include <LibGUI/ColorInput.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Window.h>

class HighlightWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(HighlightWidget)
public:
    static ErrorOr<NonnullRefPtr<HighlightWidget>> try_create();
    virtual ~HighlightWidget() override = default;

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    HighlightWidget() = default;
    ErrorOr<void> setup();

    Gfx::Color highlight_color();

    int highlight_radius();

    RefPtr<MouseSettings::HighlightPreviewWidget> m_highlight_preview;
    RefPtr<GUI::ColorInput> m_highlight_color_input;
    RefPtr<GUI::Slider> m_highlight_opacity_slider;
    RefPtr<GUI::Slider> m_highlight_radius_slider;
};
