/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/ImageDecoding.h>

namespace Web::ImageDecoding {

static RefPtr<Decoder> s_decoder;

Decoder::Decoder() = default;

Decoder::~Decoder() = default;

void Decoder::initialize(RefPtr<Decoder>&& decoder)
{
    s_decoder = move(decoder);
}

Decoder& Decoder::the()
{
    if (!s_decoder) [[unlikely]] {
        dbgln("Web::ImageDecoding::Decoder was not initialized!");
        VERIFY_NOT_REACHED();
    }
    return *s_decoder;
}

}
