/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>

namespace GUI {

class Command {
public:
    virtual ~Command() = default;

    virtual ErrorOr<void> undo() { return {}; };
    virtual ErrorOr<void> redo() { return {}; };

    virtual DeprecatedString action_text() const { return {}; }
    virtual bool merge_with(Command const&) { return false; }

protected:
    Command() = default;
};

}
