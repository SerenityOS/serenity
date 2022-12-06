/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

Symbol::Symbol(Optional<DeprecatedString> description, bool is_global)
    : m_description(move(description))
    , m_is_global(is_global)
{
}

NonnullGCPtr<Symbol> Symbol::create(VM& vm, Optional<DeprecatedString> description, bool is_global)
{
    return *vm.heap().allocate_without_realm<Symbol>(move(description), is_global);
}

}
