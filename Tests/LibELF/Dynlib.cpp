/*
 * Copyright (c) 2021, Rodrigo Tobar <rtobarc@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibLine/Span.h>

extern "C" {
int FUNCTION();
int FUNCTION()
{
    return (int)Line::Span(0, 0).beginning();
}
}
