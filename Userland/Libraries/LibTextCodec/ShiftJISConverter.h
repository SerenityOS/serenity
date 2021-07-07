/*
 * Copyright (c) 2021, Łukasz Patron <priv.luk@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace TextCodec {

u32 convert_shift_jis_to_utf8(u16 in);

}
