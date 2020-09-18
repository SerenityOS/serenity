/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/DoublyLinkedList.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Random.h>
#include <LibKeyboard/CharacterMap.h>

namespace Kernel {

class KeyboardClient;

class KeyboardDevice final : public IRQHandler
    , public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    using Event = KeyEvent;

    static void initialize();
    static KeyboardDevice& the();

    virtual ~KeyboardDevice() override;
    KeyboardDevice();

    void set_client(KeyboardClient* client) { m_client = client; }
    void set_maps(const Keyboard::CharacterMapData& character_map, const String& character_map_name);

    const String keymap_name() { return m_character_map.character_map_name(); }

    // ^CharacterDevice
    virtual KResultOr<size_t> read(FileDescription&, size_t, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> write(FileDescription&, size_t, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }

    virtual const char* purpose() const override { return class_name(); }

private:
    // ^IRQHandler
    virtual void handle_irq(const RegisterState&) override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "KeyboardDevice"; }

    void key_state_changed(u8 raw, bool pressed);
    void update_modifier(u8 modifier, bool state)
    {
        if (state)
            m_modifiers |= modifier;
        else
            m_modifiers &= ~modifier;
    }

    KeyboardClient* m_client { nullptr };
    CircularQueue<Event, 16> m_queue;
    u8 m_modifiers { 0 };
    bool m_caps_lock_on { false };
    bool m_num_lock_on { false };
    bool m_has_e0_prefix { false };
    EntropySource m_entropy_source;

    Keyboard::CharacterMap m_character_map = Keyboard::CharacterMap("en");
};

class KeyboardClient {
public:
    virtual ~KeyboardClient();
    virtual void on_key_pressed(KeyboardDevice::Event) = 0;
};

}
