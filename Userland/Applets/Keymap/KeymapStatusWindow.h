/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 * Copyright (c) 2022, Thitat Auareesuksakul <thitat@flux.ci>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/FileWatcher.h>
#include <LibGUI/Label.h>
#include <LibGUI/Window.h>
#include <LibKeyboard/Keymap.h>

class KeymapStatusWidget : public GUI::Label {
    C_OBJECT(KeymapStatusWidget);

    virtual void mousedown_event(GUI::MouseEvent& event) override;
};

class KeymapStatusWindow final : public GUI::Window {
    C_OBJECT(KeymapStatusWindow)
public:
    virtual ~KeymapStatusWindow() override = default;
    void cycle_keymaps();

private:
    virtual void wm_event(GUI::WMEvent&) override;

    KeymapStatusWindow();
    void refresh_keymaps(bool repaint_background = true);
    void set_keymap(String const& keymap);
    void set_keymap_text(String const& keymap, bool repaint_background = true);

    RefPtr<KeymapStatusWidget> m_status_widget;
    RefPtr<Core::FileWatcher> m_file_watcher;
    Vector<Keyboard::Keymap> m_keymaps;
    ssize_t m_keymap_index { -1 };
};
