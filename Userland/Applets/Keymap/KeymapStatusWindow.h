/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "KeymapStatusWidget.h"
#include <LibGUI/Label.h>
#include <LibGUI/Window.h>

class KeymapStatusWindow final : public GUI::Window {
    C_OBJECT(KeymapStatusWindow)
public:
    virtual ~KeymapStatusWindow() override = default;

private:
    virtual void wm_event(GUI::WMEvent&) override;

    KeymapStatusWindow();

    RefPtr<KeymapStatusWidget> m_status_widget;

    void set_keymap_text(ByteString const& keymap);
};
