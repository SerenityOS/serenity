/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/StringView.h>

namespace Manual {

class PageNode;

class Node : public RefCounted<Node> {
public:
    virtual ~Node() = default;

    virtual NonnullRefPtrVector<Node>& children() const = 0;
    virtual Node const* parent() const = 0;
    virtual String name() const = 0;
    virtual bool is_page() const { return false; }
    virtual bool is_open() const { return false; }
};

}
