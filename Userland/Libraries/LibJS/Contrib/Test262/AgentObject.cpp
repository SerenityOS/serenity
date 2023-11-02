/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <LibJS/Contrib/Test262/AgentObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>

#if !defined(AK_OS_WINDOWS)
#    include <unistd.h>
#else
#    include <windows.h>
#endif

namespace JS::Test262 {

AgentObject::AgentObject(Realm& realm)
    : Object(Object::ConstructWithoutPrototypeTag::Tag, realm)
{
}

void AgentObject::initialize(JS::Realm& realm)
{
    Base::initialize(realm);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, "monotonicNow", monotonic_now, 0, attr);
    define_native_function(realm, "sleep", sleep, 1, attr);
    // TODO: broadcast
    // TODO: getReport
    // TODO: start
}

JS_DEFINE_NATIVE_FUNCTION(AgentObject::monotonic_now)
{
    auto time = MonotonicTime::now();
    auto milliseconds = time.milliseconds();
    return Value(static_cast<double>(milliseconds));
}

JS_DEFINE_NATIVE_FUNCTION(AgentObject::sleep)
{
    auto milliseconds = TRY(vm.argument(0).to_i32(vm));
#if !defined(AK_OS_WINDOWS)
    ::usleep(milliseconds * 1000);
#else
    ::Sleep(milliseconds);
#endif
    return js_undefined();
}

}
