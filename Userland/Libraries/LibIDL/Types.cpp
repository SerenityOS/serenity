/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIDL/Types.h>

namespace IDL {

ParameterizedType const& Type::as_parameterized() const
{
    return verify_cast<ParameterizedType const>(*this);
}

ParameterizedType& Type::as_parameterized()
{
    return verify_cast<ParameterizedType>(*this);
}

UnionType const& Type::as_union() const
{
    return verify_cast<UnionType const>(*this);
}

UnionType& Type::as_union()
{
    return verify_cast<UnionType>(*this);
}

}
