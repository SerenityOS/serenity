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

    String action_text() const { return m_action_text; }

protected:
    Command() { }
    void set_action_text(const String& text) { m_action_text = text; }

private:
    String m_action_text;
};

}
