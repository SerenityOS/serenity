/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TextMetrics.h"

namespace Web::HTML {

RefPtr<TextMetrics> TextMetrics::create()
{
    return adopt_ref(*new TextMetrics());
}

}
