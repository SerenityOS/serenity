#pragma once

#include <AK/String.h>

class GCommand {
public:
    virtual ~GCommand();

    virtual void undo() {}
    virtual void redo() {}

    String action_text() const { return m_action_text; }

protected:
    GCommand() {}
    void set_action_text(const String& text) { m_action_text = text; }

private:
    String m_action_text;
};
