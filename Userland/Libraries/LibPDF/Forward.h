/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace PDF {

class Document;

#define ENUMERATE_DIRECT_OBJECT_TYPES(V) \
    V(StringObject, string)              \
    V(NameObject, name)                  \
    V(ArrayObject, array)                \
    V(DictObject, dict)                  \
    V(StreamObject, stream)              \
    V(IndirectObject, indirect_object)

#define ENUMERATE_OBJECT_TYPES(V)    \
    ENUMERATE_DIRECT_OBJECT_TYPES(V) \
    V(IndirectObjectRef, indirect_object_ref)

#define FORWARD_DECL(class_name, _) class class_name;
ENUMERATE_OBJECT_TYPES(FORWARD_DECL)
#undef FORWARD_DECL

}
