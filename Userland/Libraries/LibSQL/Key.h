/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Tuple.h>

namespace SQL {

class Key : public Tuple {
public:
    Key() = default;
    explicit Key(NonnullRefPtr<TupleDescriptor> const&);
    explicit Key(NonnullRefPtr<IndexDef>);
    Key(NonnullRefPtr<TupleDescriptor> const&, Serializer&);
    Key(RefPtr<IndexDef>, Serializer&);
    RefPtr<IndexDef> index() const { return m_index; }

private:
    RefPtr<IndexDef> m_index { nullptr };
};

}
