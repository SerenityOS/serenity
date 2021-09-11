/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/StringIterator.h>
#include <LibJS/Runtime/StringIteratorPrototype.h>

namespace JS {

StringIteratorPrototype::StringIteratorPrototype(GlobalObject& global_object)
    : Object(*global_object.iterator_prototype())
{
}

void StringIteratorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    define_native_function(vm.names.next, next, 0, Attribute::Configurable | Attribute::Writable);

    // 22.1.5.1.2 %StringIteratorPrototype% [ @@toStringTag ], https://tc39.es/ecma262/#sec-%stringiteratorprototype%-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "String Iterator"), Attribute::Configurable);
}

StringIteratorPrototype::~StringIteratorPrototype()
{
}

// 22.1.5.1.1 %StringIteratorPrototype%.next ( ), https://tc39.es/ecma262/#sec-%stringiteratorprototype%.next
JS_DEFINE_NATIVE_FUNCTION(StringIteratorPrototype::next)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object() || !is<StringIterator>(this_value.as_object())) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObjectOfType, "String Iterator");
        return {};
    }

    auto& this_object = this_value.as_object();
    auto& iterator = static_cast<StringIterator&>(this_object);
    if (iterator.done())
        return create_iterator_result_object(global_object, js_undefined(), true);

    auto& utf8_iterator = iterator.iterator();

    if (utf8_iterator.done()) {
        iterator.m_done = true;
        return create_iterator_result_object(global_object, js_undefined(), true);
    }

    StringBuilder builder;
    builder.append_code_point(*utf8_iterator);
    ++utf8_iterator;

    return create_iterator_result_object(global_object, js_string(vm, builder.to_string()), false);
}

}
