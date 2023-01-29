/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

class BigInt final : public Cell {
    JS_CELL(BigInt, Cell);

public:
    [[nodiscard]] static NonnullGCPtr<BigInt> create(VM&, Crypto::SignedBigInteger);

    virtual ~BigInt() override = default;

    Crypto::SignedBigInteger const& big_integer() const { return m_big_integer; }
    const DeprecatedString to_deprecated_string() const { return DeprecatedString::formatted("{}n", m_big_integer.to_base_deprecated(10)); }

private:
    explicit BigInt(Crypto::SignedBigInteger);

    Crypto::SignedBigInteger m_big_integer;
};

ThrowCompletionOr<BigInt*> number_to_bigint(VM&, Value);

}
