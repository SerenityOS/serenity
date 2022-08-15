/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/StringIterator.h>

namespace JS {

StringIterator* StringIterator::create(Realm& realm, String string)
{
    return realm.heap().allocate<StringIterator>(realm, move(string), *realm.global_object().string_iterator_prototype());
}

StringIterator::StringIterator(String string, Object& prototype)
    : Object(prototype)
    , m_string(move(string))
    , m_iterator(Utf8View(m_string).begin())
{
}

}
