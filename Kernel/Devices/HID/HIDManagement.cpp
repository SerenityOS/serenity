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
    : m_character_map("en-us", DEFAULT_CHARACTER_MAP)
{
}

void HIDManagement::set_maps(const Keyboard::CharacterMapData& character_map_data, const String& character_map_name)
{
    m_character_map.set_character_map_data(character_map_data);
    m_character_map.set_character_map_name(character_map_name);
    dbgln("New Character map '{}' passed in by client.", character_map_name);
}

UNMAP_AFTER_INIT void HIDManagement::enumerate()
{
    // FIXME: When we have USB HID support, we should ensure that we disable
    // emulation of the PS/2 controller if it was set by the BIOS.
    // If ACPI indicates we have an i8042 controller and the USB controller was
    // set to emulate PS/2, we should not initialize the PS/2 controller.
    if (kernel_command_line().disable_ps2_controller())
        return;
    if (ACPI::Parser::the() && !ACPI::Parser::the()->have_8042())
        return;
    m_i8042_controller = I8042Controller::initialize();
    m_i8042_controller->detect_devices();
    if (m_i8042_controller->mouse())
        m_hid_devices.append(m_i8042_controller->mouse().release_nonnull());

    if (m_i8042_controller->keyboard())
        m_hid_devices.append(m_i8042_controller->keyboard().release_nonnull());
}

UNMAP_AFTER_INIT void HIDManagement::initialize()
{
    VERIFY(!s_the.is_initialized());
    s_the.ensure_instance();
    s_the->enumerate();
}

HIDManagement& HIDManagement::the()
{
    return *s_the;
}

}
