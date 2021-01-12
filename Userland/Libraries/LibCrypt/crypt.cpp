/*
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
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
#include <AK/Types.h>
#include <LibCrypto/Hash/SHA2.h>
#include <string.h>
#include <unistd.h>

extern "C" {

static struct crypt_data crypt_data;

char* crypt(const char* key, const char* salt)
{
    crypt_data.initialized = true;
    return crypt_r(key, salt, &crypt_data);
}

static constexpr size_t crypt_salt_max = 16;
static constexpr size_t sha_string_length = 44;

char* crypt_r(const char* key, const char* salt, struct crypt_data* data)
{
    if (!data->initialized) {
        errno = EINVAL;
        return nullptr;
    }

    if (salt[0] == '$') {
        if (salt[1] == '5') {
            const char* salt_value = salt + 3;
            size_t salt_len = min(strcspn(salt_value, "$"), crypt_salt_max);
            size_t header_len = salt_len + 3;

            bool fits = String(salt, header_len).copy_characters_to_buffer(data->result, sizeof(data->result));
            if (!fits) {
                errno = EINVAL;
                return nullptr;
            }
            data->result[header_len] = '$';

            Crypto::Hash::SHA256 sha;
            sha.update(key);
            sha.update((const u8*)salt_value, salt_len);

            auto digest = sha.digest();
            auto string = encode_base64(ReadonlyBytes(digest.immutable_data(), digest.data_length()));

            fits = string.copy_characters_to_buffer(data->result + header_len + 1, sizeof(data->result) - header_len - 1);
            if (!fits) {
                errno = EINVAL;
                return nullptr;
            }

            return data->result;
        }
    }

    // DES crypt is not available.
    errno = EINVAL;
    return nullptr;
}
}
