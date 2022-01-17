/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TextEditor.h>
#include <LibVT/TerminalWidget.h>

class TerminalSettingsMainWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(TerminalSettingsMainWidget)
public:
    virtual void apply_settings() override;
    virtual void cancel_settings() override;

private:
    TerminalSettingsMainWidget();
    void write_back_settings() const;

    static VT::TerminalWidget::BellMode parse_bell(StringView bell_string);
    static String stringify_bell(VT::TerminalWidget::BellMode bell_mode);

    VT::TerminalWidget::BellMode m_bell_mode = VT::TerminalWidget::BellMode::Disabled;
    size_t m_max_history_size;
    bool m_show_scrollbar { true };

    VT::TerminalWidget::BellMode m_original_bell_mode;
    size_t m_original_max_history_size;
    bool m_orignal_show_scrollbar { true };
};

class TerminalSettingsViewWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(TerminalSettingsViewWidget)
public:
    virtual void apply_settings() override;
    virtual void cancel_settings() override;

private:
    TerminalSettingsViewWidget();
    void write_back_settings() const;

    RefPtr<Gfx::Font> m_font;
    float m_opacity;
    String m_color_scheme;

    RefPtr<Gfx::Font> m_original_font;
    float m_original_opacity;
    String m_original_color_scheme;
};
