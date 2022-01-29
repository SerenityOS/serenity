/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Segments.h>
#include <LibJS/Runtime/Intl/SegmentsPrototype.h>

namespace JS::Intl {

// 18.5.2 The %SegmentsPrototype% Object, https://tc39.es/ecma402/#sec-%segmentsprototype%-object
SegmentsPrototype::SegmentsPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void SegmentsPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
}

}
