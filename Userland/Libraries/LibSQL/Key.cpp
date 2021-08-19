/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>

namespace SQL {

Key::Key(NonnullRefPtr<TupleDescriptor> const& descriptor)
    : Tuple(descriptor)
{
}

Key::Key(NonnullRefPtr<IndexDef> index)
    : Tuple(index->to_tuple_descriptor())
    , m_index(index)
{
}

Key::Key(NonnullRefPtr<TupleDescriptor> const& descriptor, Serializer& serializer)
    : Tuple(descriptor, serializer)
{
}

Key::Key(RefPtr<IndexDef> index, Serializer& serializer)
    : Key(index->to_tuple_descriptor())
{
    Tuple::deserialize(serializer);
}

}
