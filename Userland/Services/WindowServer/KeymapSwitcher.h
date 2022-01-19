/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibKeyboard/CharacterMap.h>
#include <WindowServer/WMClientConnection.h>

namespace WindowServer {

class KeymapSwitcher final : public Core::Object {
    C_OBJECT(KeymapSwitcher)
public:
    virtual ~KeymapSwitcher() override;

    void refresh();

    void next_keymap();

    Function<void(String const& keymap)> on_keymap_change;

    String get_current_keymap() const;

private:
    KeymapSwitcher();

    Vector<AK::String> m_keymaps;

    void setkeymap(AK::String const&);
};

}
