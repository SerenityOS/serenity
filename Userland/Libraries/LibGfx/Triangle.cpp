/*
 * Copyright (c) 2020, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/String.h>
#include <LibGfx/Triangle.h>

namespace Gfx {

String Triangle::to_string() const
{
    return String::formatted("({},{},{})", m_a, m_b, m_c);
}

}
