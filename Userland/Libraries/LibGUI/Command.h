/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace GUI {

class Command {
public:
    virtual ~Command();

    virtual void undo() { }
    virtual void redo() { }

    virtual String action_text() const { return {}; }
    virtual bool no_action() const { return false; }
    virtual bool merge_with(Command const&) { return false; }

protected:
    Command() { }
};

}
