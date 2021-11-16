/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/Noncopyable.h>
#include <LibJS/Forward.h>

namespace JS {

class Cell {
    AK_MAKE_NONCOPYABLE(Cell);
    AK_MAKE_NONMOVABLE(Cell);

public:
    virtual void initialize(GlobalObject&) { }
    virtual ~Cell() { }

    bool is_marked() const { return m_mark; }
    void set_marked(bool b) { m_mark = b; }

#ifdef JS_TRACK_ZOMBIE_CELLS
    virtual void did_become_zombie()
    {
    }
#endif

    enum class State {
        Live,
        Dead,
#ifdef JS_TRACK_ZOMBIE_CELLS
        Zombie,
#endif
    };

    State state() const { return m_state; }
    void set_state(State state) { m_state = state; }

    virtual const char* class_name() const = 0;

    class Visitor {
    public:
        void visit(Cell* cell)
        {
            if (cell)
                visit_impl(*cell);
        }
        void visit(Value);

    protected:
        virtual void visit_impl(Cell&) = 0;
        virtual ~Visitor() = default;
    };

    virtual bool is_environment() const { return false; }
    virtual void visit_edges(Visitor&) { }

    Heap& heap() const;
    VM& vm() const;

protected:
    Cell() { }

private:
    bool m_mark : 1 { false };
    State m_state : 7 { State::Live };
};

}

template<>
struct AK::Formatter<JS::Cell> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, JS::Cell const* cell)
    {
        if (!cell)
            return Formatter<FormatString>::format(builder, "Cell{nullptr}");
        else
            return Formatter<FormatString>::format(builder, "{}({})", cell->class_name(), cell);
    }
};
