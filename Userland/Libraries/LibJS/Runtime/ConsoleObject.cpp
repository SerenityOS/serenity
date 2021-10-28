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
    u8 attr = Attribute::Writable | Attribute::Enumerable | Attribute::Configurable;
    define_native_function(vm.names.log, log, 0, attr);
    define_native_function(vm.names.debug, debug, 0, attr);
    define_native_function(vm.names.info, info, 0, attr);
    define_native_function(vm.names.warn, warn, 0, attr);
    define_native_function(vm.names.error, error, 0, attr);
    define_native_function(vm.names.trace, trace, 0, attr);
    define_native_function(vm.names.count, count, 0, attr);
    define_native_function(vm.names.countReset, count_reset, 0, attr);
    define_native_function(vm.names.clear, clear, 0, attr);
    define_native_function(vm.names.assert, assert_, 0, attr);
}

ConsoleObject::~ConsoleObject()
{
}

// 1.1.6. log(...data), https://console.spec.whatwg.org/#log
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::log)
{
    return global_object.console().log();
}

// 1.1.3. debug(...data), https://console.spec.whatwg.org/#debug
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::debug)
{
    return global_object.console().debug();
}

// 1.1.5. info(...data), https://console.spec.whatwg.org/#info
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::info)
{
    return global_object.console().info();
}

// 1.1.9. warn(...data), https://console.spec.whatwg.org/#warn
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::warn)
{
    return global_object.console().warn();
}

// 1.1.4. error(...data), https://console.spec.whatwg.org/#error
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::error)
{
    return global_object.console().error();
}

// 1.1.8. trace(...data), https://console.spec.whatwg.org/#trace
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::trace)
{
    return global_object.console().trace();
}

// 1.2.1. count(label), https://console.spec.whatwg.org/#count
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::count)
{
    return global_object.console().count();
}

// 1.2.2. countReset(label), https://console.spec.whatwg.org/#countreset
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::count_reset)
{
    return global_object.console().count_reset();
}

// 1.1.2. clear(), https://console.spec.whatwg.org/#clear
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::clear)
{
    return global_object.console().clear();
}

// 1.1.1. assert(condition, ...data), https://console.spec.whatwg.org/#assert
JS_DEFINE_NATIVE_FUNCTION(ConsoleObject::assert_)
{
    return global_object.console().assert_();
}

}
