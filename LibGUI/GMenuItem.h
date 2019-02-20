#pragma once

#include <AK/AKString.h>

class GAction;

class GMenuItem {
public:
    enum Type { Invalid, Action, Separator };

    explicit GMenuItem(Type);
    explicit GMenuItem(RetainPtr<GAction>&&);
    ~GMenuItem();

    Type type() const { return m_type; }
    String text() const;
    const GAction* action() const { return m_action.ptr(); }
    GAction* action() { return m_action.ptr(); }
    unsigned identifier() const { return m_identifier; }

private:
    Type m_type { Invalid };
    unsigned m_identifier { 0 };
    RetainPtr<GAction> m_action;
};

