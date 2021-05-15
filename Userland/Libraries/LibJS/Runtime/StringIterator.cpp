/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/StringIterator.h>

namespace JS {

StringIterator* StringIterator::create(GlobalObject& global_object, String string)
{
    return global_object.heap().allocate<StringIterator>(global_object, *global_object.string_iterator_prototype(), move(string));
}

StringIterator::StringIterator(Object& prototype, String string)
    : Object(prototype)
    , m_string(move(string))
    , m_iterator(Utf8View(m_string).begin())
{
}

StringIterator::~StringIterator()
{
}

}
