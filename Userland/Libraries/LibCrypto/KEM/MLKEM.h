/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/ByteBuffer.h>
#include <AK/Span.h>

namespace Crypto::KEM {

// The ML-KEM algorithms are described in [FIPS203].
// https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.203.pdf

namespace MLKEM {

enum class Variation : u8 {
    ML_KEM_512,
    ML_KEM_768,
    ML_KEM_1024,
};

struct Result {
    Array<u8, 32> K; // Shared secret.
    ByteBuffer c;    // Ciphertext.
};

namespace Detail {

// This is only exposed for testing purposes.
Result encapsulate_internal(Variation, ReadonlyBytes encapsulation_key, ReadonlyBytes m);

}

ErrorOr<Result> encapsulate(Variation, ReadonlyBytes encapsulation_key);

};

} // Crypto::KEM
