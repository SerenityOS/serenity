/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/NonnullOwnPtrVector.h>

class ManualNode {
public:
    virtual ~ManualNode() = default;

    virtual NonnullOwnPtrVector<ManualNode>& children() const = 0;
    virtual ManualNode const* parent() const = 0;
    virtual DeprecatedString name() const = 0;
    virtual bool is_page() const { return false; }
    virtual bool is_open() const { return false; }
};
