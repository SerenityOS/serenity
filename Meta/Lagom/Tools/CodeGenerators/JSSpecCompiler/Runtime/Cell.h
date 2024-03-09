/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"
#include "Printer.h"

namespace JSSpecCompiler::Runtime {

class Cell {
public:
    virtual ~Cell() { }

    virtual StringView type_name() const = 0;

    void dump(Printer& printer) const
    {
        // FIXME: Handle cyclic references.
        return do_dump(printer);
    }

protected:
    virtual void do_dump(Printer& printer) const = 0;
};

}
