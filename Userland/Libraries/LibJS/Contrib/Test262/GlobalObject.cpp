/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibJS/Contrib/Test262/$262Object.h>
#include <LibJS/Contrib/Test262/AgentObject.h>
#include <LibJS/Contrib/Test262/GlobalObject.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/VM.h>

namespace JS::Test262 {

void GlobalObject::initialize_global_object()
{
    Base::initialize_global_object();

    m_$262 = vm().heap().allocate<$262Object>(*this, *this);

    // https://github.com/tc39/test262/blob/master/INTERPRETING.md#host-defined-functions
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("print", print, 1, attr);
    define_direct_property("$262", m_$262, attr);
}

void GlobalObject::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_$262);
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::print)
{
    auto string = TRY(vm.argument(0).to_string(global_object));
    outln("{}", string);
    return js_undefined();
}

}
