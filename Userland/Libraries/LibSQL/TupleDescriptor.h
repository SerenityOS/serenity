/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibSQL/Type.h>

namespace SQL {

struct TupleElement {
    String name { "" };
    SQLType type { SQLType::Text };
    Order order { Order::Ascending };

    bool operator==(TupleElement const&) const = default;
};

class TupleDescriptor
    : public Vector<TupleElement>
    , public RefCounted<TupleDescriptor> {
public:
    TupleDescriptor() = default;
    ~TupleDescriptor() = default;

    [[nodiscard]] size_t data_length() const
    {
        size_t sz = sizeof(u32);
        for (auto& part : *this) {
            sz += size_of(part.type);
        }
        return sz;
    }

    [[nodiscard]] int compare_ignoring_names(TupleDescriptor const& other) const
    {
        if (size() != other.size())
            return (int)size() - (int)other.size();
        for (auto ix = 0u; ix < size(); ++ix) {
            auto elem = (*this)[ix];
            auto other_elem = other[ix];
            if ((elem.type != other_elem.type) || (elem.order != other_elem.order)) {
                return 1;
            }
        }
        return 0;
    }

    using Vector<TupleElement>::operator==;
};

}
