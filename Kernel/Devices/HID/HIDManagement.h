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
#include <LibKeyboard/CharacterMap.h>

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
    AK_MAKE_ETERNAL;

public:
    HIDManagement();
    static void initialize();
    static HIDManagement& the();

    void enumerate();

    const String& keymap_name() const { return m_character_map.character_map_name(); }
    const Keyboard::CharacterMapData& character_maps() const { return m_character_map.character_map_data(); }
    const Keyboard::CharacterMap& character_map() const { return m_character_map; }
    void set_client(KeyboardClient* client) { m_client = client; }
    void set_maps(const Keyboard::CharacterMapData& character_map, const String& character_map_name);

private:
    size_t generate_minor_device_number_for_mouse();
    size_t generate_minor_device_number_for_keyboard();

    size_t m_mouse_minor_number { 0 };
    size_t m_keyboard_minor_number { 0 };
    Keyboard::CharacterMap m_character_map;
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
