/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/Variant.h>
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
    SHA384,
    SHA512,
    MD5,
};

struct MultiHashDigestVariant {
    constexpr static size_t Size = 0;

    MultiHashDigestVariant(Empty digest)
        : m_digest(move(digest))
    {
    }

    MultiHashDigestVariant(MD5::DigestType digest)
        : m_digest(move(digest))
    {
    }

    MultiHashDigestVariant(SHA1::DigestType digest)
        : m_digest(move(digest))
    {
    }

    MultiHashDigestVariant(SHA256::DigestType digest)
        : m_digest(move(digest))
    {
    }

    MultiHashDigestVariant(SHA384::DigestType digest)
        : m_digest(move(digest))
    {
    }

    MultiHashDigestVariant(SHA512::DigestType digest)
        : m_digest(move(digest))
    {
    }

    const u8* immutable_data() const
    {
        const u8* data = nullptr;
        m_digest.visit(
            [&](const Empty&) { VERIFY_NOT_REACHED(); },
            [&](const auto& value) { data = value.immutable_data(); });
        return data;
    }

    size_t data_length()
    {
        size_t length = 0;
        m_digest.visit(
            [&](const Empty&) { VERIFY_NOT_REACHED(); },
            [&](const auto& value) { length = value.data_length(); });
        return length;
    }

    using DigestVariant = Variant<Empty, MD5::DigestType, SHA1::DigestType, SHA256::DigestType, SHA384::DigestType, SHA512::DigestType>;
    DigestVariant m_digest { Empty {} };
};

class Manager final : public HashFunction<0, MultiHashDigestVariant> {
public:
    using HashFunction::update;

    Manager()
    {
        m_pre_init_buffer = ByteBuffer::create_zeroed(0);
    }

    Manager(const Manager& other) // NOT a copy constructor!
    {
        m_pre_init_buffer = ByteBuffer::create_zeroed(0); // will not be used
        initialize(other.m_kind);
    }

    Manager(HashKind kind)
    {
        m_pre_init_buffer = ByteBuffer::create_zeroed(0);
        initialize(kind);
    }

    ~Manager()
    {
        m_algorithm = Empty {};
    }

    inline size_t digest_size() const
    {
        size_t result = 0;
        m_algorithm.visit(
            [&](const Empty&) {},
            [&](const auto& hash) { result = hash.digest_size(); });
        return result;
    }

    inline size_t block_size() const
    {
        size_t result = 0;
        m_algorithm.visit(
            [&](const Empty&) {},
            [&](const auto& hash) { result = hash.block_size(); });
        return result;
    }

    inline void initialize(HashKind kind)
    {
        if (!m_algorithm.has<Empty>()) {
            VERIFY_NOT_REACHED();
        }

        m_kind = kind;
        switch (kind) {
        case HashKind::MD5:
            m_algorithm = MD5();
            break;
        case HashKind::SHA1:
            m_algorithm = SHA1();
            break;
        case HashKind::SHA256:
            m_algorithm = SHA256();
            break;
        case HashKind::SHA384:
            m_algorithm = SHA384();
            break;
        case HashKind::SHA512:
            m_algorithm = SHA512();
            break;
        default:
        case HashKind::None:
            m_algorithm = Empty {};
            break;
        }
    }

    virtual void update(const u8* data, size_t length) override
    {
        auto size = m_pre_init_buffer.size();
        if (size) {
            m_algorithm.visit(
                [&](Empty&) {},
                [&](auto& hash) { hash.update(m_pre_init_buffer); });
        }
        m_algorithm.visit(
            [&](Empty&) { m_pre_init_buffer.append(data, length); },
            [&](auto& hash) { hash.update(data, length); });
        if (size && m_kind != HashKind::None)
            m_pre_init_buffer.clear();
    }

    virtual DigestType peek() override
    {
        DigestType result = Empty {};
        m_algorithm.visit(
            [&](Empty&) { VERIFY_NOT_REACHED(); },
            [&](auto& hash) { result = hash.peek(); });
        return result;
    }

    virtual DigestType digest() override
    {
        auto digest = peek();
        reset();
        return digest;
    }

    virtual void reset() override
    {
        m_pre_init_buffer.clear();
        m_algorithm.visit(
            [&](Empty&) {},
            [&](auto& hash) { hash.reset(); });
    }

    virtual String class_name() const override
    {
        String result;
        m_algorithm.visit(
            [&](const Empty&) { result = "UninitializedHashManager"; },
            [&](const auto& hash) { result = hash.class_name(); });
        return result;
    }

    inline bool is(HashKind kind) const
    {
        return m_kind == kind;
    }

private:
    using AlgorithmVariant = Variant<Empty, MD5, SHA1, SHA256, SHA384, SHA512>;
    AlgorithmVariant m_algorithm { Empty {} };
    HashKind m_kind { HashKind::None };
    ByteBuffer m_pre_init_buffer;
};

}
}
