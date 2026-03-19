/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/CheckBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SettingsWindow.h>
#include <LibVT/TerminalWidget.h>

namespace TerminalSettings {
class MainWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(TerminalSettingsMainWidget)
public:
    static ErrorOr<NonnullRefPtr<MainWidget>> create();

    virtual void apply_settings() override;
    virtual void cancel_settings() override;
    virtual void reset_default_values() override;

private:
    static ErrorOr<NonnullRefPtr<MainWidget>> try_create();
    MainWidget() = default;
    ErrorOr<void> setup();
    void write_back_settings() const;

    VT::TerminalWidget::BellMode m_bell_mode { VT::TerminalWidget::BellMode::Disabled };
    VT::TerminalWidget::AutoMarkMode m_automark_mode { VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt };
    bool m_confirm_close { true };

    VT::TerminalWidget::BellMode m_original_bell_mode;
    VT::TerminalWidget::AutoMarkMode m_original_automark_mode;
    bool m_original_confirm_close { true };

    RefPtr<GUI::RadioButton> m_beep_bell_radio;
    RefPtr<GUI::RadioButton> m_visual_bell_radio;
    RefPtr<GUI::RadioButton> m_no_bell_radio;
    RefPtr<GUI::RadioButton> m_automark_off_radio;
    RefPtr<GUI::RadioButton> m_automark_on_interactive_prompt_radio;
    RefPtr<GUI::CheckBox> m_confirm_close_checkbox;
};
}
