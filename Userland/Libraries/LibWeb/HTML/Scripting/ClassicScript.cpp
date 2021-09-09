/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#creating-a-classic-script
NonnullRefPtr<ClassicScript> ClassicScript::create(StringView source, JS::GlobalObject& global_object, URL base_url, MutedErrors muted_errors)
{
    // 1. If muted errors was not provided, let it be false. (NOTE: This is taken care of by the default argument.)

    // 2. If muted errors is true, then set baseURL to about:blank.
    if (muted_errors == MutedErrors::Yes)
        base_url = "about:blank";

    // FIXME: 3. If scripting is disabled for settings, then set source to the empty string.

    // 4. Let script be a new classic script that this algorithm will subsequently initialize.
    auto script = adopt_ref(*new ClassicScript(move(base_url)));

    // FIXME: 5. Set script's settings object to settings.

    // 6. Set script's base URL to baseURL. (NOTE: This was already done when constructing.)

    // FIXME: 7. Set script's fetch options to options.

    // 8. Set script's muted errors to muted errors.
    script->m_muted_errors = muted_errors;

    // FIXME: 9. Set script's parse error and error to rethrow to null.

    // 10. Let result be ParseScript(source, settings's Realm, script).
    auto result = JS::Script::parse(source, global_object);

    // FIXME: 11. If result is a list of errors, then:
    //            1. Set script's parse error and its error to rethrow to result[0].
    //            2. Return script.

    // 12. Set script's record to result.
    script->m_script_record = move(result);

    // 13. Return script.
    return script;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#run-a-classic-script
JS::Value ClassicScript::run(RethrowErrors rethrow_errors)
{
    (void)rethrow_errors;

    auto interpreter = JS::Interpreter::create_with_existing_global_object(m_script_record->global_object());
    interpreter->run(interpreter->global_object(), m_script_record->parse_node());
    auto& vm = interpreter->vm();
    if (vm.exception())
        vm.clear_exception();
    return vm.last_value();
}

ClassicScript::ClassicScript(URL base_url)
    : Script(move(base_url))
{
}

ClassicScript::~ClassicScript()
{
}

}
