/*
 * Copyright (c) 2020, Shannon Booth <shannon.ml.booth@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibGfx/Triangle.h>

namespace Gfx {

template<>
String Triangle<int>::to_string() const
{
    return String::formatted("({},{},{})", m_a, m_b, m_c);
}

template<>
String Triangle<float>::to_string() const
{
    return String::formatted("({},{},{})", m_a, m_b, m_c);
}

}
