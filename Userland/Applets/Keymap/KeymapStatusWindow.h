/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Label.h>
#include <LibGUI/Window.h>

class KeymapStatusWidget;

class KeymapStatusWindow final : public GUI::Window {
    C_OBJECT(KeymapStatusWindow)
public:
    virtual ~KeymapStatusWindow() override;

private:
    void wm_event(GUI::WMEvent&) override;

    KeymapStatusWindow();

    RefPtr<GUI::Label> m_label;

    void set_keymap_text(String const& keymap);
};
