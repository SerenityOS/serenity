/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/CircularQueue.h>
#include <AK/Error.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/UnixTypes.h>
#include <LibKeyboard/CharacterMapData.h>

namespace Kernel {

extern Atomic<bool> g_caps_lock_remapped_to_ctrl;

class HIDDevice;
class I8042Controller;
class MouseDevice;
class KeyboardDevice;
class KeyboardClient;
class HIDManagement {
    friend class KeyboardDevice;
    friend class MouseDevice;

public:
    HIDManagement();
    static void initialize();
    static HIDManagement& the();

    ErrorOr<void> enumerate();

    StringView keymap_name() const { return m_character_map_name->view(); }
    Keyboard::CharacterMapData const& character_map() const { return m_character_map; }
    u32 get_char_from_character_map(KeyEvent) const;

    void set_client(KeyboardClient* client) { m_client = client; }
    void set_maps(NonnullOwnPtr<KString> character_map_name, Keyboard::CharacterMapData const& character_map);

private:
    size_t generate_minor_device_number_for_mouse();
    size_t generate_minor_device_number_for_keyboard();

    size_t m_mouse_minor_number { 0 };
    size_t m_keyboard_minor_number { 0 };
    NonnullOwnPtr<KString> m_character_map_name;
    Keyboard::CharacterMapData m_character_map;
    KeyboardClient* m_client { nullptr };
    RefPtr<I8042Controller> m_i8042_controller;
    NonnullRefPtrVector<HIDDevice> m_hid_devices;
};

class KeyboardClient {
public:
    virtual ~KeyboardClient() = default;
    virtual void on_key_pressed(KeyEvent) = 0;
};

}
