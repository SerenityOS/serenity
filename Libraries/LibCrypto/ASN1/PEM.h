/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Span.h>
#include <LibCrypto/ASN1/ASN1.h>
#include <LibCrypto/ASN1/DER.h>

namespace Crypto {

static ByteBuffer decode_pem(ReadonlyBytes data_in, size_t cert_index = 0)
{
    size_t i { 0 };
    size_t start_at { 0 };
    size_t idx { 0 };
    size_t input_length = data_in.size();
    auto alloc_len = input_length / 4 * 3;
    auto output = ByteBuffer::create_uninitialized(alloc_len);

    for (i = 0; i < input_length; i++) {
        if ((data_in[i] == '\n') || (data_in[i] == '\r'))
            continue;

        if (data_in[i] != '-') {
            // Read entire line.
            while ((i < input_length) && (data_in[i] != '\n'))
                i++;
            continue;
        }

        if (data_in[i] == '-') {
            auto end_idx = i;
            // Read until end of line.
            while ((i < input_length) && (data_in[i] != '\n'))
                i++;
            if (start_at) {
                if (cert_index > 0) {
                    cert_index--;
                    start_at = 0;
                } else {
                    idx = decode_b64(data_in.offset(start_at), end_idx - start_at, output);
                    break;
                }
            } else
                start_at = i + 1;
        }
    }
    return output.slice(0, idx);
}

}
