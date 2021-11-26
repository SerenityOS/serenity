/*
 * Copyright (c) 2021, The SerenityOS Developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace VT {

enum CharacterSet {
    Iso_8859_1,
    Null,
    UserDefined,
    VT100,
};

class CharacterSetTranslator {
public:
    u32 translate_code_point(CharacterSet active_set, u32 code_point)
    {
        // Only translate 0x7F and lower
        if (code_point > 127)
            return code_point;

        // FIXME: implement other character sets
        if (active_set != CharacterSet::VT100)
            return code_point;

        // VT100 translation table - https://en.wikipedia.org/wiki/Box-drawing_character#Unix,_CP/M,_BBS
        switch (code_point) {
        case 0x6A:
            return 0x2518;
        case 0x6B:
            return 0x2510;
        case 0x6C:
            return 0x250C;
        case 0x6D:
            return 0x2514;
        case 0x6E:
            return 0x253C;
        case 0x71:
            return 0x2500;
        case 0x74:
            return 0x251C;
        case 0x75:
            return 0x2524;
        case 0x76:
            return 0x2534;
        case 0x77:
            return 0x252C;
        case 0x78:
            return 0x2502;
        }
        return code_point;
    }
};

}
