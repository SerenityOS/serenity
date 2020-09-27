/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <AK/StringBuilder.h>
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
    Object::initialize(global_object);

    define_native_function("next", next, 0, Attribute::Configurable | Attribute::Writable);
    define_property(global_object.vm().well_known_symbol_to_string_tag(), js_string(global_object.heap(), "String Iterator"), Attribute::Configurable);
}

StringIteratorPrototype::~StringIteratorPrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(StringIteratorPrototype::next)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object() || !this_value.as_object().is_string_iterator_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "String Iterator");
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
