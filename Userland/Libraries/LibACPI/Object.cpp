/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Object.h"

namespace ACPI::AML {

ErrorOr<Package> Package::clone() const
{
    Vector<Object> package;
    TRY(package.try_ensure_capacity(m_package.size()));
    for (auto const& object : m_package)
        package.unchecked_append(TRY(object.clone()));

    return Package(move(package));
}

ErrorOr<Object> Object::clone() const
{
    return m_data.visit(
        [](AK::Empty) -> ErrorOr<Object> { return Object(); },
        [](u64 value) -> ErrorOr<Object> { return value; },
        [](String const& value) -> ErrorOr<Object> { return String(value); },
        [](Buffer const& value) -> ErrorOr<Object> { return Buffer(value); },
        [](Package const& value) -> ErrorOr<Object> { return TRY(value.clone()); },
        [](NameString const& value) -> ErrorOr<Object> { return TRY(value.clone()); });
}

}
