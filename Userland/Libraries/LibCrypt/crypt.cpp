/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Types.h>
#include <LibCrypto/Hash/SHA2.h>
#include <crypt.h>
#include <errno.h>
#include <string.h>

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
