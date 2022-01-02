/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGfx/Alignment.h>

class TaskbarSettingsMainWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(TaskbarSettingsMainWidget)
public:
    virtual void apply_settings() override;
    virtual void cancel_settings() override;

private:
    TaskbarSettingsMainWidget();
    void write_back_settings() const;

    static Gfx::Alignment string_to_location(StringView location);
    static String location_to_string(Gfx::Alignment location);

    Gfx::Alignment m_taskbar_location = Gfx::Alignment::Bottom;
    bool m_preview_desktop = true;

    Gfx::Alignment m_original_taskbar_location;
    bool m_original_preview_desktop;
};
