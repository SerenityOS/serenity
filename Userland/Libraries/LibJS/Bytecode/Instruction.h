/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>

namespace JS::Bytecode {

class Instruction {
public:
    virtual ~Instruction();

    virtual String to_string() const = 0;
    virtual void execute(Bytecode::Interpreter&) const = 0;
};

}
