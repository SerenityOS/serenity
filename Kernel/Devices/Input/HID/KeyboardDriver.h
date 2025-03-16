/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Input/HID/ApplicationCollectionDriver.h>
#include <Kernel/Devices/Input/HID/Definitions.h>
#include <Kernel/Devices/Input/KeyboardDevice.h>

namespace Kernel::HID {

class KeyboardDriver final : public ApplicationCollectionDriver {
public:
    virtual ~KeyboardDriver() override;
    static ErrorOr<NonnullRefPtr<KeyboardDriver>> create(Device const&, ::HID::ApplicationCollection const&);

private:
    KeyboardDriver(Device const&, ::HID::ApplicationCollection const&, NonnullRefPtr<KeyboardDevice>);

    void handle_keyboard_or_keypad_change_event(u16 keycode, bool value);

    // ^ApplicationCollectionDriver
    virtual ErrorOr<void> on_report(ReadonlyBytes) override;

    NonnullRefPtr<KeyboardDevice> m_keyboard_device;
    EntropySource m_entropy_source;

    // FIXME: We can't use an AK::Bitmap here, since we need to iterate over all key state bits that are different in the new state to generate key up/down events,
    //        but AK::Bitmap doesn't have a function for this.
    // "Keyboard Right GUI" is the highest currently defined Keyboard/Keypad Page Usage ID.
    static constexpr size_t key_state_bitmap_size_in_bits = to_underlying(Usage::KeyboardRightGUI) + 1;
    using KeyStateBitmapElement = FlatPtr;
    static constexpr size_t key_state_bitmap_bits_per_element = NumericLimits<KeyStateBitmapElement>::digits();
    static constexpr size_t key_state_bitmap_array_element_count = ceil_div(key_state_bitmap_size_in_bits, key_state_bitmap_bits_per_element);
    Array<KeyStateBitmapElement, key_state_bitmap_array_element_count> m_key_state {};
};

}
