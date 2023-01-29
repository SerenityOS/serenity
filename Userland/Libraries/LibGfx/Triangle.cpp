/*
 * Copyright (c) 2020, Shannon Booth <shannon.ml.booth@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibGfx/Triangle.h>

namespace Gfx {

template<>
DeprecatedString Triangle<int>::to_deprecated_string() const
{
    return DeprecatedString::formatted("({},{},{})", m_a, m_b, m_c);
}

template<>
DeprecatedString Triangle<float>::to_deprecated_string() const
{
    return DeprecatedString::formatted("({},{},{})", m_a, m_b, m_c);
}

}
