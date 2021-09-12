/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DoubleClickArrowWidget.h"

class MouseWidget final : public GUI::Widget {
    C_OBJECT(MouseWidget)
public:
    virtual ~MouseWidget() override;

    void update_window_server();
    void reset_default_values();

private:
    MouseWidget();

    RefPtr<GUI::HorizontalSlider> m_speed_slider;
    RefPtr<GUI::Label> m_speed_label;
    RefPtr<GUI::SpinBox> m_scroll_length_spinbox;
    RefPtr<GUI::HorizontalSlider> m_double_click_speed_slider;
    RefPtr<GUI::Label> m_double_click_speed_label;
    RefPtr<MouseSettings::DoubleClickArrowWidget> m_double_click_arrow_widget;
};
