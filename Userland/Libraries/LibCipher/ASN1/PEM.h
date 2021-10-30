/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <LibCipher/ASN1/ASN1.h>
#include <LibCipher/ASN1/DER.h>

namespace Crypto {

ByteBuffer decode_pem(ReadonlyBytes);

}
