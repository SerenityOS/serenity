/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

PrimitiveString::PrimitiveString(String string)
    : m_string(move(string))
{
}

PrimitiveString::~PrimitiveString()
{
}

PrimitiveString* js_string(Heap& heap, String string)
{
    if (string.is_empty())
        return &heap.vm().empty_string();

    if (string.length() == 1 && (u8)string.characters()[0] < 0x80)
        return &heap.vm().single_ascii_character_string(string.characters()[0]);

    return heap.allocate_without_global_object<PrimitiveString>(move(string));
}

PrimitiveString* js_string(VM& vm, String string)
{
    return js_string(vm.heap(), move(string));
}

}
