/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <LibJS/Console.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ConsoleObject::ConsoleObject()
    : Object(interpreter().global_object().object_prototype())
{
    put_native_function("log", log);
    put_native_function("debug", debug);
    put_native_function("info", info);
    put_native_function("warn", warn);
    put_native_function("error", error);
    put_native_function("trace", trace);
    put_native_function("count", count);
    put_native_function("countReset", count_reset);
    put_native_function("clear", clear);
}

ConsoleObject::~ConsoleObject()
{
}

Value ConsoleObject::log(Interpreter& interpreter)
{
    return interpreter.console().log();
}

Value ConsoleObject::debug(Interpreter& interpreter)
{
    return interpreter.console().debug();
}

Value ConsoleObject::info(Interpreter& interpreter)
{
    return interpreter.console().info();
}

Value ConsoleObject::warn(Interpreter& interpreter)
{
    return interpreter.console().warn();
}

Value ConsoleObject::error(Interpreter& interpreter)
{
    return interpreter.console().error();
}

Value ConsoleObject::trace(Interpreter& interpreter)
{
    return interpreter.console().trace();
}

Value ConsoleObject::count(Interpreter& interpreter)
{
    return interpreter.console().count();
}

Value ConsoleObject::count_reset(Interpreter& interpreter)
{
    return interpreter.console().count_reset();
}

Value ConsoleObject::clear(Interpreter& interpreter)
{
    return interpreter.console().clear();
}

}
