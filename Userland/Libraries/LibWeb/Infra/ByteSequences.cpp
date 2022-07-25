/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibWeb/Infra/ByteSequences.h>

namespace Web::Infra {

// https://infra.spec.whatwg.org/#byte-lowercase
void byte_lowercase(ByteBuffer& bytes)
{
    // To byte-lowercase a byte sequence, increase each byte it contains, in the range 0x41 (A) to 0x5A (Z), inclusive, by 0x20.
    for (size_t i = 0; i < bytes.size(); ++i)
        bytes[i] = to_ascii_lowercase(bytes[i]);
}

// https://infra.spec.whatwg.org/#byte-uppercase
void byte_uppercase(ByteBuffer& bytes)
{
    // To byte-uppercase a byte sequence, subtract each byte it contains, in the range 0x61 (a) to 0x7A (z), inclusive, by 0x20.
    for (size_t i = 0; i < bytes.size(); ++i)
        bytes[i] = to_ascii_uppercase(bytes[i]);
}

}
