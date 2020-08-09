/*
 * Copyright (c) 2020, Tom Lebreux <tomlebreux@hotmail.com>
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

#include <AK/TestSuite.h>

#include <AK/Base64.h>
#include <AK/ByteBuffer.h>
#include <AK/String.h>

TEST_CASE(test_decode)
{
    auto decode_equal = [&](const char* input, const char* expected) {
        auto decoded = decode_base64(StringView(input));
        EXPECT(String::copy(decoded) == String(expected));
    };

    decode_equal("", "");
    decode_equal("Zg==", "f");
    decode_equal("Zm8=", "fo");
    decode_equal("Zm9v", "foo");
    decode_equal("Zm9vYg==", "foob");
    decode_equal("Zm9vYmE=", "fooba");
    decode_equal("Zm9vYmFy", "foobar");
}

TEST_CASE(test_encode)
{
    auto encode_equal = [&](const char* input, const char* expected) {
        auto encoded = encode_base64({ input, strlen(input) });
        EXPECT(encoded == String(expected));
    };

    encode_equal("", "");
    encode_equal("f", "Zg==");
    encode_equal("fo", "Zm8=");
    encode_equal("foo", "Zm9v");
    encode_equal("foob", "Zm9vYg==");
    encode_equal("fooba", "Zm9vYmE=");
    encode_equal("foobar", "Zm9vYmFy");
}

TEST_MAIN(Base64)
