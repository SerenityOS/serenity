/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Property.h>

namespace Core {

Property::Property(String name, Function<JsonValue()> getter, Function<bool(JsonValue const&)> setter)
    : m_name(move(name))
    , m_getter(move(getter))
    , m_setter(move(setter))
{
}

}
