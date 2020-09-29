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
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ConsoleObject::ConsoleObject(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void ConsoleObject::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
    define_native_function("log", log);
    define_native_function("debug", debug);
    define_native_function("info", info);
    define_native_function("warn", warn);
    define_native_function("error", error);
    define_native_function("trace", trace);
    define_native_function("count", count);
    define_native_function("countReset", count_reset);
    define_native_function("clear", clear);
}

ConsoleObject::~ConsoleObject()
{
}

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::log)
{
    return global_object.console().log();
}

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::debug)
{
    return global_object.console().debug();
}

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::info)
{
    return global_object.console().info();
}

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::warn)
{
    return global_object.console().warn();
}

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::error)
{
    return global_object.console().error();
}

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::trace)
{
    return global_object.console().trace();
}

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::count)
{
    return global_object.console().count();
}

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::count_reset)
{
    return global_object.console().count_reset();
}

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::clear)
{
    return global_object.console().clear();
}

}
