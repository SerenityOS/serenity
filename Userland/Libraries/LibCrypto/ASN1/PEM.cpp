/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
            if (b64decoded.is_error()) {
                dbgln("Failed to decode PEM: {}", b64decoded.error().string_literal());
                return {};
            }
            if (decoded.try_append(b64decoded.value().data(), b64decoded.value().size()).is_error()) {
                dbgln("Failed to decode PEM, likely OOM condition");
                return {};
            }
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
