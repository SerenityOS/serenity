/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypedTransfer.h>
#include <Kernel/Devices/Input/HID/Device.h>
#include <Kernel/Devices/Input/HID/KeyboardDriver.h>
#include <Kernel/Devices/Input/HID/KeyboardKeymap.h>
#include <Kernel/Devices/Input/Management.h>

#include <LibHID/ReportParser.h>

namespace Kernel::HID {

KeyboardDriver::~KeyboardDriver()
{
    InputManagement::the().detach_standalone_input_device(*m_keyboard_device);
}

ErrorOr<NonnullRefPtr<KeyboardDriver>> KeyboardDriver::create(Device const& device, ::HID::ApplicationCollection const& application_collection)
{
    auto keyboard_device = TRY(::KeyboardDevice::try_to_initialize());
    auto handler = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) KeyboardDriver(device, application_collection, move(keyboard_device))));
    InputManagement::the().attach_standalone_input_device(*handler->m_keyboard_device);
    return handler;
}

KeyboardDriver::KeyboardDriver(Device const& device, ::HID::ApplicationCollection const& application_collection, NonnullRefPtr<::KeyboardDevice> keyboard_device)
    : ApplicationCollectionDriver(device, application_collection)
    , m_keyboard_device(move(keyboard_device))
{
}

void KeyboardDriver::handle_keyboard_or_keypad_change_event(u16 usage_id, bool value)
{
    if (usage_id == 0)
        return;

    m_entropy_source.add_random_event(usage_id);

    m_keyboard_device->update_modifier(Mod_Keypad, false);

    RawKeyEvent raw_key_event;
    raw_key_event.is_press_down = value == 1;
    raw_key_event.scancode = usage_id;

    auto usage = static_cast<Usage>(usage_id | (to_underlying(UsagePage::KeyboardOrKeypad) << 16));

    using enum Usage;
    switch (usage) {
    case KeyboardLeftControl:
    case KeyboardRightControl:
        m_keyboard_device->update_modifier(Mod_Ctrl, raw_key_event.is_press());
        break;

    case KeyboardLeftShift:
    case KeyboardRightShift:
        m_keyboard_device->update_modifier(Mod_Shift, raw_key_event.is_press());
        break;

    case KeyboardLeftAlt:
        m_keyboard_device->update_modifier(Mod_Alt, raw_key_event.is_press());
        break;

    case KeyboardRightAlt:
        m_keyboard_device->update_modifier(Mod_AltGr, raw_key_event.is_press());
        break;

    case KeyboardLeftGUI:
    case KeyboardRightGUI:
        m_keyboard_device->update_modifier(Mod_Super, raw_key_event.is_press());
        break;

    default:
        break;
    };

    if (usage >= Usage::KeypadNumlock && usage <= Usage::KeypadDot)
        m_keyboard_device->update_modifier(Mod_Keypad, true);

    bool use_shifted_key_map = (m_keyboard_device->modifiers() & Mod_Shift) != 0 && usage_id < shifted_keyboard_keypad_page_keymap.size();
    auto key_map = use_shifted_key_map ? Span<KeyCodeEntry const>(shifted_keyboard_keypad_page_keymap) : Span<KeyCodeEntry const>(unshifted_keyboard_keypad_page_keymap);

    if (usage_id >= key_map.size()) {
        dbgln_if(HID_DEBUG, "HID: Unknown Keyboard Application Collection Usage ID: {:#x}", usage_id);
        return;
    }

    raw_key_event.code_entry = key_map[usage_id];

    KeyEvent key_event {
        .key = raw_key_event.code_entry.key_code,
        .map_entry_index = raw_key_event.code_entry.map_entry_index,
        .scancode = raw_key_event.scancode,
        .flags = raw_key_event.is_press() ? static_cast<u8>(Is_Press) : static_cast<u8>(0),
    };

    if (m_keyboard_device->num_lock_on() && (m_keyboard_device->modifiers() & Mod_Shift) == 0) {
        if (usage >= Usage::Keypad1 && usage <= Usage::KeypadDot) {
            auto index = (usage_id - (to_underlying(Usage::Keypad1) & 0xffff));
            static constexpr auto numpad_key_map = to_array<KeyCodeEntry>({
                { Key_1, 0x02 },
                { Key_2, 0x03 },
                { Key_3, 0x04 },
                { Key_4, 0x05 },
                { Key_5, 0x06 },
                { Key_6, 0x07 },
                { Key_7, 0x08 },
                { Key_8, 0x09 },
                { Key_9, 0x0a },
                { Key_0, 0x0b },
                { Key_Period, 0x34 },
            });

            key_event.key = numpad_key_map[index].key_code;
            key_event.map_entry_index = numpad_key_map[index].map_entry_index;
        }
    }

    if (key_event.key == Key_Invalid) {
        dbgln_if(HID_DEBUG, "HID: Unknown Keyboard Application Collection Usage ID: {:#x}", usage_id);
        return;
    }

    m_keyboard_device->handle_input_event(key_event);
}

ErrorOr<void> KeyboardDriver::on_report(ReadonlyBytes report_data)
{
    decltype(m_key_state) new_key_state {};

    TRY(::HID::parse_input_report(m_device.report_descriptor(), m_application_collection, report_data, [&new_key_state](::HID::Field const& field, i64 value) -> ErrorOr<IterationDecision> {
        u32 usage = 0;
        if (field.is_array) {
            if (!field.usage_minimum.has_value())
                return Error::from_errno(ENOTSUP); // TODO: What are we supposed to do here?

            usage = value + field.usage_minimum.value();
            value = 1;
        } else {
            usage = field.usage.value();
        }

        if ((usage >> 16) != to_underlying(UsagePage::KeyboardOrKeypad)) {
            dbgln_if(HID_DEBUG, "HID: Unknown Keyboard Application Collection Usage: {:#x}", usage);
            return IterationDecision::Continue;
        }

        u16 usage_id = usage & 0xffff;

        if (usage_id >= new_key_state.size()) {
            dbgln_if(HID_DEBUG, "HID: Unknown Keyboard/Keypad Page Usage ID: {:#x}", usage_id);
            return IterationDecision::Continue;
        }

        if (value == 1)
            new_key_state[usage_id / key_state_bitmap_bits_per_element] |= 1zu << (usage_id % key_state_bitmap_bits_per_element);

        return IterationDecision::Continue;
    }));

    // FIXME: Optimize this.
    for (size_t i = 0; i < key_state_bitmap_size_in_bits; i++) {
        auto old_bitmap_element = m_key_state[i / key_state_bitmap_bits_per_element];
        auto new_bitmap_element = new_key_state[i / key_state_bitmap_bits_per_element];

        bool old_value = old_bitmap_element & (1zu << (i % key_state_bitmap_bits_per_element));
        bool new_value = new_bitmap_element & (1zu << (i % key_state_bitmap_bits_per_element));

        if (old_value != new_value)
            handle_keyboard_or_keypad_change_event(i, new_value);
    }

    AK::TypedTransfer<KeyStateBitmapElement>::copy(m_key_state.data(), new_key_state.data(), m_key_state.size());

    return {};
}

}
