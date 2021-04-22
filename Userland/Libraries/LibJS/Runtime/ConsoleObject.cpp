/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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
    auto& vm = this->vm();
    Object::initialize(global_object);
    define_native_function(vm.names.log, log);
    define_native_function(vm.names.debug, debug);
    define_native_function(vm.names.info, info);
    define_native_function(vm.names.warn, warn);
    define_native_function(vm.names.error, error);
    define_native_function(vm.names.trace, trace);
    define_native_function(vm.names.count, count);
    define_native_function(vm.names.countReset, count_reset);
    define_native_function(vm.names.clear, clear);
    define_native_function(vm.names.assert, assert_);
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

JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::assert_)
{
    return global_object.console().assert_();
}

}
