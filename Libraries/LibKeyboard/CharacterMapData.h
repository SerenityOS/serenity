/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include <AK/Types.h>

#define CHAR_MAP_SIZE 0x80

namespace Keyboard {

struct CharacterMapData {
    u32 map[CHAR_MAP_SIZE];
    u32 shift_map[CHAR_MAP_SIZE];
    u32 alt_map[CHAR_MAP_SIZE];
    u32 altgr_map[CHAR_MAP_SIZE];
};

// clang-format off
static const CharacterMapData default_character_map =
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
};
// clang-format on

}
