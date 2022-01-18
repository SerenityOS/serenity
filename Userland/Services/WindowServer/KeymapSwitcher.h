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

namespace WindowServer {

class KeymapSwitcher final : public Core::Object {
    C_OBJECT(KeymapSwitcher)
public:
    static KeymapSwitcher& the();

    virtual ~KeymapSwitcher() override;

    void refresh();

    void next_keymap();

private:
    KeymapSwitcher();

    Vector<AK::String> m_keymaps;

    void setkeymap(AK::String const&);
    String get_current_keymap() const;
};

}
