/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/RegExpStringIterator.h>

namespace JS {

// 22.2.7.1 CreateRegExpStringIterator ( R, S, global, fullUnicode ), https://tc39.es/ecma262/#sec-createregexpstringiterator
RegExpStringIterator* RegExpStringIterator::create(GlobalObject& global_object, Object& regexp_object, Utf16String string, bool global, bool unicode)
{
    return global_object.heap().allocate<RegExpStringIterator>(global_object, *global_object.regexp_string_iterator_prototype(), regexp_object, move(string), global, unicode);
}

RegExpStringIterator::RegExpStringIterator(Object& prototype, Object& regexp_object, Utf16String string, bool global, bool unicode)
    : Object(prototype)
    , m_regexp_object(regexp_object)
    , m_string(move(string))
    , m_global(global)
    , m_unicode(unicode)
{
}

void RegExpStringIterator::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_regexp_object);
}

}
