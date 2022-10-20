/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/Noncopyable.h>
#include <AK/StringView.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/GCPtr.h>

namespace JS {

#define JS_CELL(class_, base_class)                \
public:                                            \
    using Base = base_class;                       \
    virtual StringView class_name() const override \
    {                                              \
        return #class_##sv;                        \
    }                                              \
    friend class JS::Heap;

class Cell {
    AK_MAKE_NONCOPYABLE(Cell);
    AK_MAKE_NONMOVABLE(Cell);

public:
    virtual void initialize(Realm&) { }
    virtual ~Cell() = default;

    bool is_marked() const { return m_mark; }
    void set_marked(bool b) { m_mark = b; }

    enum class State {
        Live,
        Dead,
    };

    State state() const { return m_state; }
    void set_state(State state) { m_state = state; }

    virtual StringView class_name() const = 0;

    class Visitor {
    public:
        void visit(Cell* cell)
        {
            if (cell)
                visit_impl(*cell);
        }
        void visit(Cell& cell)
        {
            visit_impl(cell);
        }
        template<typename T>
        void visit(GCPtr<T> cell)
        {
            if (cell)
                visit_impl(*cell.ptr());
        }
        template<typename T>
        void visit(NonnullGCPtr<T> cell)
        {
            visit_impl(*cell.ptr());
        }
        void visit(Value);

    protected:
        virtual void visit_impl(Cell&) = 0;
        virtual ~Visitor() = default;
    };

    virtual bool is_environment() const { return false; }
    virtual void visit_edges(Visitor&) { }

    // This will be called on unmarked objects by the garbage collector in a separate pass before destruction.
    virtual void finalize() { }

    Heap& heap() const;
    VM& vm() const;

protected:
    Cell() = default;

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
            return builder.put_string("Cell{nullptr}"sv);
        return Formatter<FormatString>::format(builder, "{}({})"sv, cell->class_name(), cell);
    }
};
