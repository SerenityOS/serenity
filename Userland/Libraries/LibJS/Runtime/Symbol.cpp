/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

Symbol::Symbol(Optional<String> description, bool is_global)
    : m_description(move(description))
    , m_is_global(is_global)
{
}

Symbol::~Symbol()
{
}

Symbol* js_symbol(Heap& heap, Optional<String> description, bool is_global)
{
    return heap.allocate_without_global_object<Symbol>(move(description), is_global);
}

Symbol* js_symbol(VM& vm, Optional<String> description, bool is_global)
{
    return js_symbol(vm.heap(), move(description), is_global);
}

}
