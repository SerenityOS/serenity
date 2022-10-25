/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/CircularQueue.h>
#include <AK/Error.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Devices/HID/HIDDevice.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Library/NonnullLockRefPtrVector.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Locking/SpinlockProtected.h>
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
    static ErrorOr<void> initialize();
    static HIDManagement& the();

    ErrorOr<void> enumerate();

    struct KeymapData {
        KeymapData();
        NonnullOwnPtr<KString> character_map_name;
        Keyboard::CharacterMapData character_map;
    };

    SpinlockProtected<KeymapData>& keymap_data() { return m_keymap_data; }

    u32 get_char_from_character_map(KeyEvent) const;

    void set_client(KeyboardClient* client);
    void set_maps(NonnullOwnPtr<KString> character_map_name, Keyboard::CharacterMapData const& character_map);

private:
    size_t generate_minor_device_number_for_mouse();
    size_t generate_minor_device_number_for_keyboard();

    SpinlockProtected<KeymapData> m_keymap_data { LockRank::None };
    size_t m_mouse_minor_number { 0 };
    size_t m_keyboard_minor_number { 0 };
    KeyboardClient* m_client { nullptr };
#if ARCH(I386) || ARCH(X86_64)
    LockRefPtr<I8042Controller> m_i8042_controller;
#endif
    NonnullLockRefPtrVector<HIDDevice> m_hid_devices;
    Spinlock m_client_lock { LockRank::None };
};

class KeyboardClient {
public:
    virtual ~KeyboardClient() = default;
    virtual void on_key_pressed(KeyEvent) = 0;
};

}
