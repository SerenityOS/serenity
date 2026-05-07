/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Widget.h>
#include <LibVT/TerminalWidget.h>

namespace TerminalSettings {
class ViewWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(TerminalSettingsViewWidget)
public:
    static ErrorOr<NonnullRefPtr<ViewWidget>> try_create();
    static ErrorOr<NonnullRefPtr<ViewWidget>> create();

    virtual void apply_settings() override;
    virtual void cancel_settings() override;
    virtual void reset_default_values() override;

private:
    ViewWidget() = default;
    ErrorOr<void> setup();
    void write_back_settings() const;

    RefPtr<Gfx::Font const> m_font;
    float m_opacity;
    ByteString m_color_scheme;
    VT::CursorShape m_cursor_shape { VT::CursorShape::Block };
    bool m_cursor_is_blinking_set { true };
    size_t m_max_history_size;
    bool m_show_scrollbar { true };

    RefPtr<Gfx::Font const> m_original_font;
    float m_original_opacity;
    ByteString m_original_color_scheme;
    VT::CursorShape m_original_cursor_shape;
    bool m_original_cursor_is_blinking_set;
    size_t m_original_max_history_size;
    bool m_original_show_scrollbar { true };

    RefPtr<GUI::HorizontalOpacitySlider> m_opacity_slider;
    RefPtr<GUI::Label> m_font_label;
    RefPtr<GUI::Widget> m_font_selection;
    RefPtr<GUI::CheckBox> m_use_default_font_checkbox;
    RefPtr<GUI::RadioButton> m_cursor_block_radio;
    RefPtr<GUI::RadioButton> m_cursor_underline_radio;
    RefPtr<GUI::RadioButton> m_cursor_bar_radio;
    RefPtr<GUI::CheckBox> m_cursor_blinking_checkbox;
    RefPtr<GUI::SpinBox> m_history_size_spinbox;
    RefPtr<GUI::CheckBox> m_show_scrollbar_checkbox;
};
}
