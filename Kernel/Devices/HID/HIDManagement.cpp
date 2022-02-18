/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/Devices/HID/I8042Controller.h>
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

UNMAP_AFTER_INIT HIDManagement::HIDManagement()
    : m_character_map_name(KString::must_create("en-us"sv))
    , m_character_map(DEFAULT_CHARACTER_MAP)
{
}

void HIDManagement::set_maps(NonnullOwnPtr<KString> character_map_name, Keyboard::CharacterMapData const& character_map_data)
{
    m_character_map_name = move(character_map_name);
    m_character_map = character_map_data;
    dbgln("New Character map '{}' passed in by client.", m_character_map_name);
}

UNMAP_AFTER_INIT ErrorOr<void> HIDManagement::enumerate()
{
    // FIXME: When we have USB HID support, we should ensure that we disable
    // emulation of the PS/2 controller if it was set by the BIOS.
    // If ACPI indicates we have an i8042 controller and the USB controller was
    // set to emulate PS/2, we should not initialize the PS/2 controller.
    if (kernel_command_line().disable_ps2_controller())
        return {};
    m_i8042_controller = I8042Controller::initialize();

    // Note: If ACPI is disabled or doesn't indicate that we have an i8042, we
    // still perform a manual existence check via probing, which is relevant on
    // QEMU, for example. This probing check is known to not work on bare metal
    // in all cases, so if we can get a 'yes' from ACPI, we skip it.
    auto has_i8042_controller = false;
    if (ACPI::Parser::the() && ACPI::Parser::the()->have_8042())
        has_i8042_controller = true;
    else if (m_i8042_controller->check_existence_via_probing({}))
        has_i8042_controller = true;

    // Note: If we happen to not have i8042 just return "gracefully" for now.
    if (!has_i8042_controller)
        return {};
    TRY(m_i8042_controller->detect_devices());
    if (m_i8042_controller->mouse())
        m_hid_devices.append(m_i8042_controller->mouse().release_nonnull());

    if (m_i8042_controller->keyboard())
        m_hid_devices.append(m_i8042_controller->keyboard().release_nonnull());
    return {};
}

UNMAP_AFTER_INIT void HIDManagement::initialize()
{
    VERIFY(!s_the.is_initialized());
    s_the.ensure_instance();
    // FIXME: Propagate errors back to init to deal with them.
    MUST(s_the->enumerate());
}

HIDManagement& HIDManagement::the()
{
    return *s_the;
}

u32 HIDManagement::get_char_from_character_map(KeyEvent event) const
{
    auto modifiers = event.modifiers();
    auto index = event.scancode & 0xFF; // Index is last byte of scan code.
    auto caps_lock_on = event.caps_lock_on;

    u32 code_point;
    if (modifiers & Mod_Alt)
        code_point = m_character_map.alt_map[index];
    else if ((modifiers & Mod_Shift) && (modifiers & Mod_AltGr))
        code_point = m_character_map.shift_altgr_map[index];
    else if (modifiers & Mod_Shift)
        code_point = m_character_map.shift_map[index];
    else if (modifiers & Mod_AltGr)
        code_point = m_character_map.altgr_map[index];
    else
        code_point = m_character_map.map[index];

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
    }

    return code_point;
}

}
