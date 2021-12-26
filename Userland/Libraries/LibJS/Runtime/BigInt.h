/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

class BigInt final : public Cell {
public:
    BigInt(Crypto::SignedBigInteger);
    virtual ~BigInt();

    const Crypto::SignedBigInteger& big_integer() const { return m_big_integer; }
    const String to_string() const { return String::formatted("{}n", m_big_integer.to_base10()); }

private:
    virtual const char* class_name() const override { return "BigInt"; }

    Crypto::SignedBigInteger m_big_integer;
};

BigInt* js_bigint(Heap&, Crypto::SignedBigInteger);

}
