/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Tuple.h>

namespace SQL {

class Key : public Tuple {
public:
    Key() = default;
    explicit Key(TupleDescriptor const&);
    explicit Key(RefPtr<IndexDef>);
    Key(TupleDescriptor const&, ByteBuffer&, size_t& offset);
    Key(RefPtr<IndexDef>, ByteBuffer&, size_t& offset);
    RefPtr<IndexDef> index() const { return m_index; }
    [[nodiscard]] virtual size_t data_length() const override { return Tuple::data_length() + sizeof(u32); }

private:
    RefPtr<IndexDef> m_index;
};

}
