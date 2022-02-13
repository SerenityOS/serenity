/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Clipboard.h>

namespace GUI {

Clipboard& Clipboard::the()
{
    static Clipboard s_the;
    return s_the;
}

}
