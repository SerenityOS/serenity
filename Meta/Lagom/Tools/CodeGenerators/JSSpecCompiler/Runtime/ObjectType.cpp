/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Runtime/ObjectType.h"

namespace JSSpecCompiler::Runtime {

void ObjectType::do_dump(Printer& printer) const
{
    printer.format("ObjectType");
}

}
