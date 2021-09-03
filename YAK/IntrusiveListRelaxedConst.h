/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/IntrusiveList.h>

namespace YAK {

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
class IntrusiveListRelaxedConst : public IntrusiveList<T, Container, member> {
    YAK_MAKE_NONCOPYABLE(IntrusiveListRelaxedConst);
    YAK_MAKE_NONMOVABLE(IntrusiveListRelaxedConst);

public:
    using IntrusiveList<T, Container, member>::IntrusiveList;

    using Iterator = typename IntrusiveList<T, Container, member>::Iterator;

    Iterator begin() const { return const_cast<IntrusiveListRelaxedConst*>(this)->IntrusiveList<T, Container, member>::begin(); }
    Iterator end() const { return Iterator {}; }
};

}

using YAK::IntrusiveListRelaxedConst;
