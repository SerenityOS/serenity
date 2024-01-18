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
    C_OBJECT_ABSTRACT(TerminalSettingsMainWidget)
public:
    static ErrorOr<NonnullRefPtr<TerminalSettingsMainWidget>> try_create();

    virtual void apply_settings() override;
    virtual void cancel_settings() override;

private:
    TerminalSettingsMainWidget() = default;
    ErrorOr<void> setup();
    void write_back_settings() const;

    static VT::TerminalWidget::BellMode parse_bell(StringView bell_string);
    static ByteString stringify_bell(VT::TerminalWidget::BellMode bell_mode);

    VT::TerminalWidget::BellMode m_bell_mode { VT::TerminalWidget::BellMode::Disabled };
    bool m_confirm_close { true };

    VT::TerminalWidget::BellMode m_original_bell_mode;
    bool m_orignal_confirm_close { true };
};
