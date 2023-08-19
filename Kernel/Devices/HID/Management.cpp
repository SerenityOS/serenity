/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <AK/Singleton.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/ISABus/I8042Controller.h>
#endif
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Devices/HID/Device.h>
#include <Kernel/Devices/HID/Management.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Sections.h>

namespace Kernel {

Atomic<bool> g_caps_lock_remapped_to_ctrl;
static Singleton<HIDManagement> s_the;

// clang-format off
static constexpr Keyboard::CharacterMapData DEFAULT_CHARACTER_MAP =
{
    .map = {
        0,    '\033',    '1',    '2',    '3',    '4',    '5',    '6',    '7',    '8',    '9',    '0',    '-',    '=',    0x08,
                '\t',    'q',    'w',    'e',    'r',    't',    'y',    'u',    'i',    'o',    'p',    '[',    ']',    '\n',
                   0,    'a',    's',    'd',    'f',    'g',    'h',    'j',    'k',    'l',    ';',   '\'',    '`',       0,
                '\\',    'z',    'x',    'c',    'v',    'b',    'n',    'm',    ',',    '.',    '/',      0,    '*',       0,
                 ' ',      0,      0,
     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '\\', 0, 0, 0,

    },

    .shift_map = {
        0,    '\033',    '!',    '@',    '#',    '$',    '%',    '^',    '&',    '*',    '(',    ')',    '_',    '+',    0x08,
                '\t',    'Q',    'W',    'E',    'R',    'T',    'Y',    'U',    'I',    'O',    'P',    '{',    '}',    '\n',
                   0,    'A',    'S',    'D',    'F',    'G',    'H',    'J',    'K',    'L',    ':',    '"',    '~',       0,
                 '|',    'Z',    'X',    'C',    'V',    'B',    'N',    'M',    '<',    '>',    '?',      0,    '*',       0,
                 ' ',      0,      0,
     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '|',  0, 0, 0,

    },

    .alt_map = {
        0,    '\033',    '1',    '2',    '3',    '4',    '5',    '6',    '7',    '8',    '9',    '0',    '-',    '=',    0x08,
                '\t',    'q',    'w',    'e',    'r',    't',    'y',    'u',    'i',    'o',    'p',    '[',    ']',    '\n',
                   0,    'a',    's',    'd',    'f',    'g',    'h',    'j',    'k',    'l',    ';',   '\'',    '`',       0,
                '\\',    'z',    'x',    'c',    'v',    'b',    'n',    'm',    ',',    '.',    '/',      0,    '*',       0,
                 ' ',      0,      0,

     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '\\', 0, 0, 0,

    },

    .altgr_map = {
        0,    '\033',    '1',    '2',    '3',    '4',    '5',    '6',    '7',    '8',    '9',    '0',    '-',    '=',    0x08,
                '\t',    'q',    'w',    'e',    'r',    't',    'y',    'u',    'i',    'o',    'p',    '[',    ']',    '\n',
                   0,    'a',    's',    'd',    'f',    'g',    'h',    'j',    'k',    'l',    ';',   '\'',    '`',       0,
                '\\',    'z',    'x',    'c',    'v',    'b',    'n',    'm',    ',',    '.',    '/',      0,    '*',       0,
                 ' ',      0,      0,
     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '\\', 0, 0, 0,
    },

    .shift_altgr_map = {
        0,    '\033',    '1',    '2',    '3',    '4',    '5',    '6',    '7',    '8',    '9',    '0',    '-',    '=',    0x08,
                '\t',    'q',    'w',    'e',    'r',    't',    'y',    'u',    'i',    'o',    'p',    '[',    ']',    '\n',
                   0,    'a',    's',    'd',    'f',    'g',    'h',    'j',    'k',    'l',    ';',   '\'',    '`',       0,
                '\\',    'z',    'x',    'c',    'v',    'b',    'n',    'm',    ',',    '.',    '/',      0,    '*',       0,
                 ' ',      0,      0,
     //60                            70                                               80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1', '2', '3', '0', '.', 0, 0, '\\', 0, 0, 0,
    },
};
// clang-format on

void HIDManagement::set_client(KeyboardClient* client)
{
    SpinlockLocker locker(m_client_lock);
    m_client = client;
}

size_t HIDManagement::generate_minor_device_number_for_mouse()
{
    // FIXME: Lock this to prevent race conditions with hot-plugging devices!
    return m_mouse_minor_number++;
}
size_t HIDManagement::generate_minor_device_number_for_keyboard()
{
    // FIXME: Lock this to prevent race conditions with hot-plugging devices!
    return m_keyboard_minor_number++;
}

UNMAP_AFTER_INIT HIDManagement::KeymapData::KeymapData()
    : character_map_name(KString::must_create("en-us"sv))
    , character_map(DEFAULT_CHARACTER_MAP)
{
}

UNMAP_AFTER_INIT HIDManagement::HIDManagement()
{
}

void HIDManagement::set_maps(NonnullOwnPtr<KString> character_map_name, Keyboard::CharacterMapData const& character_map_data)
{
    m_keymap_data.with([&](auto& keymap_data) {
        keymap_data.character_map_name = move(character_map_name);
        keymap_data.character_map = character_map_data;
        dbgln("New Character map '{}' passed in by client.", keymap_data.character_map_name);
    });
}

UNMAP_AFTER_INIT ErrorOr<void> HIDManagement::enumerate()
{
    // FIXME: When we have USB HID support, we should ensure that we disable
    // emulation of the PS/2 controller if it was set by the BIOS.
    // If ACPI indicates we have an i8042 controller and the USB controller was
    // set to emulate PS/2, we should not initialize the PS/2 controller.
#if ARCH(X86_64)
    auto has_i8042_controller = false;
    auto i8042_controller = TRY(I8042Controller::create());
    switch (kernel_command_line().i8042_presence_mode()) {
    case I8042PresenceMode::Automatic: {
        // Note: If ACPI is disabled or doesn't indicate that we have an i8042, we
        // still perform a manual existence check via probing, which is relevant on
        // QEMU, for example. This probing check is known to not work on bare metal
        // in all cases, so if we can get a 'yes' from ACPI, we skip it.
        if (ACPI::Parser::the() && ACPI::Parser::the()->have_8042())
            has_i8042_controller = true;
        else if (i8042_controller->check_existence_via_probing({}))
            has_i8042_controller = true;
        break;
    }
    case I8042PresenceMode::Force: {
        has_i8042_controller = true;
        break;
    }
    case I8042PresenceMode::None: {
        break;
    }
    case I8042PresenceMode::AggressiveTest: {
        if (i8042_controller->check_existence_via_probing({}))
            has_i8042_controller = true;
        break;
    }
    }

    // Note: If we happen to not have i8042 just return "gracefully" for now.
    if (!has_i8042_controller)
        return {};
    if (auto result_or_error = i8042_controller->detect_devices(); result_or_error.is_error())
        return {};
    m_hid_serial_io_controllers.with([&](auto& list) {
        list.append(i8042_controller);
    });
#endif
    return {};
}

UNMAP_AFTER_INIT ErrorOr<void> HIDManagement::initialize()
{
    VERIFY(!s_the.is_initialized());
    s_the.ensure_instance();
    return s_the->enumerate();
}

HIDManagement& HIDManagement::the()
{
    return *s_the;
}

u32 HIDManagement::get_char_from_character_map(KeyEvent event, bool num_lock_on) const
{
    auto modifiers = event.modifiers();
    auto index = event.scancode & 0xFF; // Index is last byte of scan code.
    auto caps_lock_on = event.caps_lock_on;

    u32 code_point = 0;
    m_keymap_data.with([&](auto& keymap_data) {
        if (modifiers & Mod_Alt)
            code_point = keymap_data.character_map.alt_map[index];
        else if ((modifiers & Mod_Shift) && (modifiers & Mod_AltGr))
            code_point = keymap_data.character_map.shift_altgr_map[index];
        else if (modifiers & Mod_Shift)
            code_point = keymap_data.character_map.shift_map[index];
        else if (modifiers & Mod_AltGr)
            code_point = keymap_data.character_map.altgr_map[index];
        else
            code_point = keymap_data.character_map.map[index];
    });

    if (caps_lock_on && (modifiers == 0 || modifiers == Mod_Shift)) {
        if (code_point >= 'a' && code_point <= 'z')
            code_point &= ~0x20;
        else if (code_point >= 'A' && code_point <= 'Z')
            code_point |= 0x20;
    }

    if (event.e0_prefix && event.key == Key_Slash) {
        // If Key_Slash (scancode = 0x35) mapped to other form "/", we fix num pad key of "/" with this case.
        code_point = '/';
    } else if (event.e0_prefix && event.key != Key_Return) {
        // Except for `keypad-/` and 'keypad-return', all e0 scan codes are not actually characters. i.e., `keypad-0` and
        // `Insert` have the same scancode except for the prefix, but insert should not have a code_point.
        code_point = 0;
    } else if (!num_lock_on && !event.e0_prefix && event.scancode >= 0x47 && event.scancode <= 0x53 && event.key != Key_Minus && event.key != Key_Plus) {
        // When Num Lock is off, some numpad keys have the same function as some of the extended keys like Home, End, PgDown, arrows etc.
        // These keys should have the code_point set to 0.
        code_point = 0;
    }

    return code_point;
}

}
