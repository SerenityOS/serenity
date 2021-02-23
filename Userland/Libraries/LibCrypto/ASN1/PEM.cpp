/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <AK/Base64.h>
#include <AK/GenericLexer.h>
#include <LibCrypto/ASN1/PEM.h>

namespace Crypto {

ByteBuffer decode_pem(ReadonlyBytes data)
{
    GenericLexer lexer { data };
    ByteBuffer decoded;

    // FIXME: Parse multiple.
    enum {
        PreStartData,
        Started,
        Ended,
    } state { PreStartData };
    while (!lexer.is_eof()) {
        switch (state) {
        case PreStartData:
            if (lexer.consume_specific("-----BEGIN"))
                state = Started;
            lexer.consume_line();
            break;
        case Started: {
            if (lexer.consume_specific("-----END")) {
                state = Ended;
                lexer.consume_line();
                break;
            }
            auto b64decoded = decode_base64(lexer.consume_line().trim_whitespace(TrimMode::Right));
            decoded.append(b64decoded.data(), b64decoded.size());
            break;
        }
        case Ended:
            lexer.consume_all();
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    return decoded;
}

}
