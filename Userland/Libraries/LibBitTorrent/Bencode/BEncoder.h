/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BTypes.h"
#include <AK/Stream.h>

namespace BitTorrent {

class BEncoder {
public:
    static ErrorOr<void> bencode(BEncodingType const&, Stream&);
};
}
