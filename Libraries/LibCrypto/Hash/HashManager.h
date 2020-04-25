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

#include <AK/Optional.h>
#include <LibCrypto/Hash/HashFunction.h>
#include <LibCrypto/Hash/MD5.h>
#include <LibCrypto/Hash/SHA1.h>
#include <LibCrypto/Hash/SHA2.h>

namespace Crypto {
namespace Hash {

enum class HashKind {
    None,
    SHA1,
    SHA256,
    SHA512,
    MD5,
};

struct MultiHashDigestVariant {

    constexpr static size_t Size = 0;

    MultiHashDigestVariant(SHA1::DigestType digest)
        : sha1(digest)
        , kind(HashKind::SHA1)
    {
    }

    MultiHashDigestVariant(SHA256::DigestType digest)
        : sha256(digest)
        , kind(HashKind::SHA256)
    {
    }

    MultiHashDigestVariant(SHA512::DigestType digest)
        : sha512(digest)
        , kind(HashKind::SHA512)
    {
    }

    MultiHashDigestVariant(MD5::DigestType digest)
        : md5(digest)
        , kind(HashKind::MD5)
    {
    }

    const u8* immutable_data() const
    {
        switch (kind) {
        case HashKind::MD5:
            return md5.value().immutable_data();
        case HashKind::SHA1:
            return sha1.value().immutable_data();
        case HashKind::SHA256:
            return sha256.value().immutable_data();
        case HashKind::SHA512:
            return sha512.value().immutable_data();
        default:
        case HashKind::None:
            ASSERT_NOT_REACHED();
            break;
        }
    }

    size_t data_length()
    {
        switch (kind) {
        case HashKind::MD5:
            return md5.value().data_length();
        case HashKind::SHA1:
            return sha1.value().data_length();
        case HashKind::SHA256:
            return sha256.value().data_length();
        case HashKind::SHA512:
            return sha512.value().data_length();
        default:
        case HashKind::None:
            ASSERT_NOT_REACHED();
            break;
        }
    }

    Optional<SHA1::DigestType> sha1;
    Optional<SHA256::DigestType> sha256;
    Optional<SHA512::DigestType> sha512;
    Optional<MD5::DigestType> md5;
    HashKind kind { HashKind::None };
};

class Manager final : public HashFunction<0, MultiHashDigestVariant> {
public:
    Manager()
    {
        m_pre_init_buffer = ByteBuffer::create_zeroed(0);
    }

    Manager(const Manager& other) // NOT a copy constructor!
    {
        m_pre_init_buffer = ByteBuffer::create_zeroed(0); // will not be used
        initialise(other.m_kind);
    }

    Manager(HashKind kind)
    {
        m_pre_init_buffer = ByteBuffer::create_zeroed(0);
        initialize(kind);
    }

    ~Manager()
    {
    }

    virtual void update(const ByteBuffer& buffer) override { update(buffer.data(), buffer.size()); };
    virtual void update(const StringView& string) override { update((const u8*)string.characters_without_null_termination(), string.length()); };
    inline size_t digest_size() const
    {
        switch (m_kind) {
        case HashKind::MD5:
            return md5.value().digest_size();
        case HashKind::SHA1:
            return sha1.value().digest_size();
        case HashKind::SHA256:
            return sha256.value().digest_size();
        case HashKind::SHA512:
            return sha512.value().digest_size();
        default:
        case HashKind::None:
            return 0;
        }
    }
    inline size_t block_size() const
    {
        switch (m_kind) {
        case HashKind::MD5:
            return m_md5->block_size();
        case HashKind::SHA1:
            return m_sha1->block_size();
        case HashKind::SHA256:
            return m_sha256->block_size();
        case HashKind::SHA512:
            return m_sha512->block_size();
        default:
        case HashKind::None:
            return 0;
        }
    }
    inline void initialize(HashKind kind)
    {
        if (m_kind != HashKind::None) {
            ASSERT_NOT_REACHED();
        }

        m_kind = kind;
        switch (kind) {
        case HashKind::MD5:
            md5 = MD5 {};
            break;
        case HashKind::SHA1:
            sha1 = SHA1 {};
            break;
        case HashKind::SHA256:
            sha256 = SHA256 {};
            break;
        case HashKind::SHA512:
            sha512 = SHA512 {};
            break;
        default:
        case HashKind::None:
            break;
        }
    }

    virtual void update(const u8* data, size_t length) override
    {
        switch (m_kind) {
        case HashKind::MD5:
            if (m_pre_init_buffer.size())
                md5.value().update(m_pre_init_buffer);
            md5.value().update(data, length);
            break;
        case HashKind::SHA1:
            if (m_pre_init_buffer.size())
                sha1.value().update(m_pre_init_buffer);
            sha1.value().update(data, length);
            break;
        case HashKind::SHA256:
            if (m_pre_init_buffer.size())
                sha256.value().update(m_pre_init_buffer);
            sha256.value().update(data, length);
            break;
        case HashKind::SHA512:
            if (m_pre_init_buffer.size())
                sha512.value().update(m_pre_init_buffer);
            sha512.value().update(data, length);
            break;
        default:
        case HashKind::None:
            m_pre_init_buffer.append(data, length);
            return;
        }
        m_pre_init_buffer.clear();
    }

    virtual DigestType peek() override
    {
        switch (m_kind) {
        case HashKind::MD5:
            return { md5.value().peek() };
        case HashKind::SHA1:
            return { sha1.value().peek() };
        case HashKind::SHA256:
            return { sha256.value().peek() };
        case HashKind::SHA512:
            return { sha512.value().peek() };
        default:
        case HashKind::None:
            ASSERT_NOT_REACHED();
            break;
        }
    }

    virtual DigestType digest() override
    {
        auto digest = peek();
        reset();
        return digest;
    }

    virtual void reset() override
    {
        switch (m_kind) {
        case HashKind::MD5:
            md5.value().reset();
            break;
        case HashKind::SHA1:
            sha1.value().reset();
            break;
        case HashKind::SHA256:
            sha256.value().reset();
            break;
        case HashKind::SHA512:
            sha512.value().reset();
            break;
        default:
        case HashKind::None:
            break;
        }
    }

    virtual String class_name() const override
    {
        switch (m_kind) {
        case HashKind::MD5:
            return md5.value().class_name();
        case HashKind::SHA1:
            return sha1.value().class_name();
        case HashKind::SHA256:
            return sha256.value().class_name();
        case HashKind::SHA512:
            return sha512.value().class_name();
        default:
        case HashKind::None:
            return "UninitializedHashManager";
        }
    }

    inline bool is(HashKind kind) const
    {
        return m_kind == kind;
    }

private:
    Optional<SHA1> sha1;
    Optional<SHA256> sha256;
    Optional<SHA512> sha512;
    Optional<MD5> md5;
    HashKind m_kind { HashKind::None };
    ByteBuffer m_pre_init_buffer;
};

}
}
