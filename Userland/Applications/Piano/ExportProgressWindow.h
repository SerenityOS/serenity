/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Event.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Label.h>
#include <LibGUI/Progressbar.h>

class ExportProgressWindow : public GUI::Dialog {
    C_OBJECT(ExportProgressWindow);

public:
    virtual ~ExportProgressWindow() override = default;

    ErrorOr<void> initialize();

    virtual void timer_event(Core::TimerEvent&) override;

    void set_filename(StringView filename);

private:
    ExportProgressWindow(Window& parent_window, Atomic<int>& wav_percent_written);

    Atomic<int>& m_wav_percent_written;

    RefPtr<GUI::HorizontalProgressbar> m_progress_bar;
    RefPtr<GUI::Label> m_label;
};
