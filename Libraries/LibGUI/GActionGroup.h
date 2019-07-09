#pragma once

#include <AK/HashTable.h>
#include <AK/Weakable.h>

class GAction;

class GActionGroup : public Weakable<GActionGroup> {
public:
    GActionGroup() {}
    ~GActionGroup() {}

    void add_action(GAction&);
    void remove_action(GAction&);

    bool is_exclusive() const { return m_exclusive; }
    void set_exclusive(bool exclusive) { m_exclusive = exclusive; }

    bool is_unchecking_allowed() const { return m_unchecking_allowed; }
    void set_unchecking_allowed(bool unchecking_allowed) { m_unchecking_allowed = unchecking_allowed; }

    template<typename C>
    void for_each_action(C callback)
    {
        for (auto& it : m_actions) {
            if (callback(*it) == IterationDecision::Break)
                break;
        }
    }

private:
    HashTable<GAction*> m_actions;
    bool m_exclusive { false };
    bool m_unchecking_allowed { false };
};
