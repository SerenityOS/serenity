/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace PDF {

class Document;
class Object;

#define ENUMERATE_OBJECT_TYPES(V) \
    V(StringObject, string)       \
    V(NameObject, name)           \
    V(ArrayObject, array)         \
    V(DictObject, dict)           \
    V(StreamObject, stream)       \
    V(IndirectValue, indirect_value)

#define FORWARD_DECL(class_name, _) class class_name;
ENUMERATE_OBJECT_TYPES(FORWARD_DECL)
#undef FORWARD_DECL

template<typename T>
concept IsObject = IsBaseOf<Object, T>;

template<typename T>
concept IsValuePrimitive = IsSame<T, bool> || IsSame<T, int> || IsSame<T, float>;

template<typename T>
concept IsValueType = IsValuePrimitive<T> || IsObject<T>;

template<IsValueType T>
using UnwrappedValueType = Conditional<IsObject<T>, NonnullRefPtr<T>, T>;

}
