/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DoubleClickArrowWidget.h"
#include <LibGUI/CheckBox.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/Window.h>

namespace MouseSettings {
class MouseWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(MouseWidget)
public:
    static ErrorOr<NonnullRefPtr<MouseWidget>> try_create();
    ErrorOr<void> initialize();

    virtual ~MouseWidget() override = default;

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    MouseWidget() = default;

    void update_speed_label();
    void update_double_click_speed_label();
    void update_switch_buttons_image_label();

    RefPtr<GUI::HorizontalSlider> m_speed_slider;
    RefPtr<GUI::Label> m_speed_label;
    RefPtr<GUI::SpinBox> m_scroll_length_spinbox;
    RefPtr<GUI::HorizontalSlider> m_double_click_speed_slider;
    RefPtr<GUI::Label> m_double_click_speed_label;
    RefPtr<GUI::CheckBox> m_switch_buttons_checkbox;
    RefPtr<GUI::ImageWidget> m_switch_buttons_image;
    RefPtr<GUI::CheckBox> m_natural_scroll_checkbox;
    RefPtr<MouseSettings::DoubleClickArrowWidget> m_double_click_arrow_widget;
};
}
