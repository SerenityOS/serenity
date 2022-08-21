/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Contrib/Test262/$262Object.h>
#include <LibJS/Contrib/Test262/AgentObject.h>
#include <LibJS/Contrib/Test262/GlobalObject.h>
#include <LibJS/Contrib/Test262/IsHTMLDDA.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Script.h>

namespace JS::Test262 {

$262Object::$262Object(Realm& realm)
    : Object(Object::ConstructWithoutPrototypeTag::Tag, realm)
{
}

void $262Object::initialize(JS::Realm& realm)
{
    Base::initialize(realm);

    m_agent = vm().heap().allocate<AgentObject>(realm, realm);
    m_is_htmldda = vm().heap().allocate<IsHTMLDDA>(realm, realm);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("clearKeptObjects", clear_kept_objects, 0, attr);
    define_native_function("createRealm", create_realm, 0, attr);
    define_native_function("detachArrayBuffer", detach_array_buffer, 1, attr);
    define_native_function("evalScript", eval_script, 1, attr);

    define_direct_property("agent", m_agent, attr);
    define_direct_property("gc", realm.global_object().get_without_side_effects("gc"), attr);
    define_direct_property("global", &realm.global_object(), attr);
    define_direct_property("IsHTMLDDA", m_is_htmldda, attr);
}

void $262Object::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_agent);
    visitor.visit(m_is_htmldda);
}

JS_DEFINE_NATIVE_FUNCTION($262Object::clear_kept_objects)
{
    vm.finish_execution_generation();
    return js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION($262Object::create_realm)
{
    auto* realm = Realm::create(vm);
    VERIFY(realm);
    auto* realm_global_object = vm.heap().allocate_without_realm<GlobalObject>(*realm);
    VERIFY(realm_global_object);
    realm->set_global_object(realm_global_object, nullptr);
    realm_global_object->set_associated_realm(*realm);
    realm_global_object->initialize_global_object();
    return Value(realm_global_object->$262());
}

JS_DEFINE_NATIVE_FUNCTION($262Object::detach_array_buffer)
{
    auto array_buffer = vm.argument(0);
    if (!array_buffer.is_object() || !is<ArrayBuffer>(array_buffer.as_object()))
        return vm.throw_completion<TypeError>();

    auto& array_buffer_object = static_cast<ArrayBuffer&>(array_buffer.as_object());
    TRY(JS::detach_array_buffer(global_object, array_buffer_object, vm.argument(1)));
    return js_null();
}

JS_DEFINE_NATIVE_FUNCTION($262Object::eval_script)
{
    auto source = TRY(vm.argument(0).to_string(vm));
    auto script_or_error = Script::parse(source, *vm.current_realm());
    if (script_or_error.is_error())
        return vm.throw_completion<SyntaxError>(script_or_error.error()[0].to_string());
    TRY(vm.interpreter().run(script_or_error.value()));
    return js_undefined();
}

}
