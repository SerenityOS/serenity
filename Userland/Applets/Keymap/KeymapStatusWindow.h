/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Label.h>
#include <LibGUI/Window.h>

class KeymapStatusWidget : public GUI::Label {
    C_OBJECT(KeymapStatusWidget);

    virtual void mousedown_event(GUI::MouseEvent& event) override;
};
class KeymapStatusWindow final : public GUI::Window {
    C_OBJECT(KeymapStatusWindow)
public:
    virtual ~KeymapStatusWindow() override;

private:
    virtual void wm_event(GUI::WMEvent&) override;

    KeymapStatusWindow();

    RefPtr<KeymapStatusWidget> m_status_widget;

    void set_keymap_text(String const& keymap);
};
