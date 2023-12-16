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

char* crypt(char const* key, char const* salt)
{
    crypt_data.initialized = true;
    return crypt_r(key, salt, &crypt_data);
}

static constexpr size_t crypt_salt_max = 16;

char* crypt_r(char const* key, char const* salt, struct crypt_data* data)
{
    if (!data->initialized) {
        errno = EINVAL;
        return nullptr;
    }

    // We only support SHA-256 at the moment
    if (salt[0] != '$' || salt[1] != '5') {
        errno = EINVAL;
        return nullptr;
    }

    char const* salt_value = salt + 3;
    size_t salt_len = min(strcspn(salt_value, "$"), crypt_salt_max);
    size_t header_len = salt_len + 3;

    bool fits = ByteString(salt, header_len).copy_characters_to_buffer(data->result, sizeof(data->result));
    if (!fits) {
        errno = EINVAL;
        return nullptr;
    }
    data->result[header_len] = '$';

    Crypto::Hash::SHA256 sha;
    sha.update(StringView { key, strlen(key) });
    sha.update(reinterpret_cast<u8 const*>(salt_value), salt_len);

    auto digest = sha.digest();
    auto string_or_error = encode_base64({ digest.immutable_data(), digest.data_length() });
    if (string_or_error.is_error()) {
        errno = ENOMEM;
        return nullptr;
    }

    auto string = string_or_error.value().bytes_as_string_view();
    fits = string.copy_characters_to_buffer(data->result + header_len + 1, sizeof(data->result) - header_len - 1);
    if (!fits) {
        errno = EINVAL;
        return nullptr;
    }

    return data->result;
}
}
