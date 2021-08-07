/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>

namespace AK {

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
class IntrusiveListRelaxedConst : public IntrusiveList<T, Container, member> {
    AK_MAKE_NONCOPYABLE(IntrusiveListRelaxedConst);
    AK_MAKE_NONMOVABLE(IntrusiveListRelaxedConst);

public:
    using IntrusiveList<T, Container, member>::IntrusiveList;

    using Iterator = typename IntrusiveList<T, Container, member>::Iterator;

    Iterator begin() const { return const_cast<IntrusiveListRelaxedConst*>(this)->IntrusiveList<T, Container, member>::begin(); }
    Iterator end() const { return Iterator {}; }
};

}

using AK::IntrusiveListRelaxedConst;
