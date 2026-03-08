/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Cipher.h"

namespace SSH {

Cipher::Cipher(u8 block_size)
    : m_block_size(block_size)
{
}

ErrorOr<void> Cipher::decrypt(Bytes bytes)
{
    if (bytes.size() % m_block_size != 0)
        return Error::from_string_literal("Can't decipher message of invalid block size");

    decrypt_impl(bytes);
    return {};
}

ErrorOr<void> Cipher::encrypt(Bytes bytes)
{
    if (bytes.size() % m_block_size != 0)
        return Error::from_string_literal("Can't obfuscate message of invalid block size");

    encrypt_impl(bytes);
    return {};
}

} // SSH
