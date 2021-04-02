/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/KResult.h>
#include <Kernel/SpinLock.h>
#include <Kernel/UnixTypes.h>
#include <LibKeyboard/CharacterMap.h>

namespace Kernel {

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
    virtual ~KeyboardClient();
    virtual void on_key_pressed(KeyEvent) = 0;
};

}
