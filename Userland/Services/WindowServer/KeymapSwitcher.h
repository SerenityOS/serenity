/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/FileWatcher.h>
#include <LibKeyboard/CharacterMap.h>
#include <WindowServer/WMConnectionFromClient.h>

namespace WindowServer {

class KeymapSwitcher final : public Core::EventReceiver {
    C_OBJECT(KeymapSwitcher)
public:
    virtual ~KeymapSwitcher() override = default;

    void next_keymap();

    Function<void(DeprecatedString const& keymap)> on_keymap_change;

    DeprecatedString get_current_keymap() const;

    void set_keymap(AK::DeprecatedString const&);

private:
    void refresh();

    KeymapSwitcher();

    Vector<AK::DeprecatedString> m_keymaps;

    RefPtr<Core::FileWatcher> m_file_watcher;

    char const* m_keyboard_config = "/etc/Keyboard.ini";
};

}
