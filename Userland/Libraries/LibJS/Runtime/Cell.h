/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/Noncopyable.h>
#include <AK/String.h>
#include <AK/TypeCasts.h>
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

    bool is_live() const { return m_live; }
    void set_live(bool b) { m_live = b; }

    virtual const char* class_name() const = 0;

    class Visitor {
    public:
        void visit(Cell*);
        void visit(Value);

    protected:
        virtual void visit_impl(Cell*) = 0;
        virtual ~Visitor() = default;
    };

    virtual void visit_edges(Visitor&) { }

    Heap& heap() const;
    VM& vm() const;

protected:
    Cell() { }

private:
    bool m_mark { false };
    bool m_live { true };
};

}

template<>
struct AK::Formatter<JS::Cell> : AK::Formatter<FormatString> {
    void format(FormatBuilder& builder, const JS::Cell* cell)
    {
        if (!cell)
            Formatter<FormatString>::format(builder, "Cell{nullptr}");
        else
            Formatter<FormatString>::format(builder, "{}({})", cell->class_name(), cell);
    }
};
