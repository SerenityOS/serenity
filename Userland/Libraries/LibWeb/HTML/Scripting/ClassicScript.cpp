/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/ElapsedTimer.h>
#include <LibJS/Interpreter.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#creating-a-classic-script
NonnullRefPtr<ClassicScript> ClassicScript::create(String filename, StringView source, JS::Realm& realm, AK::URL base_url, MutedErrors muted_errors)
{
    // 1. If muted errors was not provided, let it be false. (NOTE: This is taken care of by the default argument.)

    // 2. If muted errors is true, then set baseURL to about:blank.
    if (muted_errors == MutedErrors::Yes)
        base_url = "about:blank";

    // FIXME: 3. If scripting is disabled for settings, then set source to the empty string.

    // 4. Let script be a new classic script that this algorithm will subsequently initialize.
    auto script = adopt_ref(*new ClassicScript(move(base_url), move(filename)));

    // FIXME: 5. Set script's settings object to settings.

    // 6. Set script's base URL to baseURL. (NOTE: This was already done when constructing.)

    // FIXME: 7. Set script's fetch options to options.

    // 8. Set script's muted errors to muted errors.
    script->m_muted_errors = muted_errors;

    // FIXME: 9. Set script's parse error and error to rethrow to null.

    // 10. Let result be ParseScript(source, settings's Realm, script).
    auto parse_timer = Core::ElapsedTimer::start_new();
    auto result = JS::Script::parse(source, realm, script->filename());
    dbgln_if(HTML_SCRIPT_DEBUG, "ClassicScript: Parsed {} in {}ms", script->filename(), parse_timer.elapsed());

    // 11. If result is a list of errors, then:

    if (result.is_error()) {
        // FIXME:  1. Set script's parse error and its error to rethrow to result[0].

        // 2. Return script.
        return script;
    }

    // 12. Set script's record to result.
    script->m_script_record = result.release_value();

    // 13. Return script.
    return script;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#run-a-classic-script
JS::Value ClassicScript::run(RethrowErrors rethrow_errors)
{
    if (!m_script_record) {
        // FIXME: Throw a SyntaxError per the spec.
        dbgln("ClassicScript: Unable to run script {}", filename());
        return {};
    }

    dbgln_if(HTML_SCRIPT_DEBUG, "ClassicScript: Running script {}", filename());
    (void)rethrow_errors;

    auto timer = Core::ElapsedTimer::start_new();

    // 6. Otherwise, set evaluationStatus to ScriptEvaluation(script's record).
    // FIXME: Interpreter::run doesn't currently return a JS::Completion.
    auto interpreter = JS::Interpreter::create_with_existing_realm(m_script_record->realm());

    auto result = interpreter->run(*m_script_record);

    // FIXME: If ScriptEvaluation does not complete because the user agent has aborted the running script, leave evaluationStatus as null.

    dbgln_if(HTML_SCRIPT_DEBUG, "ClassicScript: Finished running script {}, Duration: {}ms", filename(), timer.elapsed());
    if (result.is_error()) {
        // FIXME: Propagate error according to the spec.
        interpreter->vm().clear_exception();
        return {};
    }
    return result.value();
}

ClassicScript::ClassicScript(AK::URL base_url, String filename)
    : Script(move(base_url), move(filename))
{
}

ClassicScript::~ClassicScript()
{
}

}
