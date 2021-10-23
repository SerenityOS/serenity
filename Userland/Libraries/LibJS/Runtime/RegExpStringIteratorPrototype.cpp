/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/RegExpPrototype.h>
#include <LibJS/Runtime/RegExpStringIteratorPrototype.h>
#include <LibJS/Runtime/Utf16String.h>

namespace JS {

RegExpStringIteratorPrototype::RegExpStringIteratorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.iterator_prototype())
{
}

void RegExpStringIteratorPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
    auto& vm = this->vm();

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.next, next, 0, attr);

    // 22.2.7.2.2 %RegExpStringIteratorPrototype% [ @@toStringTag ], https://tc39.es/ecma262/#sec-%regexpstringiteratorprototype%-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "RegExp String Iterator"), Attribute::Configurable);
}

// 22.2.7.2.1 %RegExpStringIteratorPrototype%.next ( ), https://tc39.es/ecma262/#sec-%regexpstringiteratorprototype%.next
JS_DEFINE_NATIVE_FUNCTION(RegExpStringIteratorPrototype::next)
{
    // For details, see the 'closure' of: https://tc39.es/ecma262/#sec-createregexpstringiterator
    auto* iterator = TRY(typed_this_value(global_object));
    if (iterator->done())
        return create_iterator_result_object(global_object, js_undefined(), true);

    auto match = TRY(regexp_exec(global_object, iterator->regexp_object(), iterator->string()));

    if (match.is_null()) {
        iterator->set_done();
        return create_iterator_result_object(global_object, js_undefined(), true);
    }

    if (!iterator->global()) {
        iterator->set_done();
        return create_iterator_result_object(global_object, match, false);
    }

    auto* match_object = TRY(match.to_object(global_object));
    auto match_string_value = TRY(match_object->get(0));
    auto match_string = TRY(match_string_value.to_string(global_object));
    if (match_string.is_empty()) {
        auto last_index_value = TRY(iterator->regexp_object().get(vm.names.lastIndex));
        auto last_index = TRY(last_index_value.to_length(global_object));

        last_index = advance_string_index(iterator->string().view(), last_index, iterator->unicode());

        TRY(iterator->regexp_object().set(vm.names.lastIndex, Value(last_index), Object::ShouldThrowExceptions::Yes));
    }

    return create_iterator_result_object(global_object, match, false);
}

}
