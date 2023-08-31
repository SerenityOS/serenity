/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/PBT/RandomRun.h>

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Variant.h>

template<typename T>
struct Generated {
    RandomRun run; // run corresponding to the value
    T value;
};

struct Rejected {
    StringView reason;
};

template<typename T>
class GenResult {
public:
    GenResult(GenResult<T>&& rhs)
        : m_result(rhs.m_result)
    {
    }
    GenResult(GenResult<T> const& rhs)
        : m_result(rhs.m_result)
    {
    }
    static GenResult<T> generated(RandomRun run, T value) { return GenResult<T>(Generated<T> { run, value }); }
    static GenResult<T> rejected(StringView reason) { return GenResult<T>(Rejected { reason }); }

    template<typename OnGenerated, typename OnRejected>
    auto visit(OnGenerated on_generated, OnRejected on_rejected)
    {
        return m_result.visit(
            [&](Generated<T> generated) { return on_generated(generated); },
            [&](Rejected rejected) { return on_rejected(rejected); });
    }

    bool is_generated() const { return m_result.template has<Generated<T>>(); }
    bool is_rejected() const { return m_result.template has<Rejected>(); }
    Generated<T> get_generated() const { return m_result.template get<Generated<T>>(); }
    Rejected get_rejected() const { return m_result.template get<Rejected>(); }

private:
    Variant<Generated<T>, Rejected> m_result;

    explicit GenResult<T>(Variant<Generated<T>, Rejected> result)
        : m_result(result)
    {
    }
};
