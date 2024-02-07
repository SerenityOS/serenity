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

namespace TerminalSettings {
class MainWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(TerminalSettingsMainWidget)
public:
    static ErrorOr<NonnullRefPtr<MainWidget>> create();

    virtual void apply_settings() override;
    virtual void cancel_settings() override;

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
    bool m_orignal_confirm_close { true };
};
}
