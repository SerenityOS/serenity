/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Square.h"

void Square::set_value(int value)
{
    if (m_fixed) {
        return;
    }
    m_value = value;
};

void Square::set_answer(int value) { m_answer = value; };

bool Square::is_correct() { return (m_value == m_answer); }
