/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibJS/Interpreter.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/Environments.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#creating-a-classic-script
NonnullRefPtr<ClassicScript> ClassicScript::create(String filename, StringView source, EnvironmentSettingsObject& environment_settings_object, AK::URL base_url, MutedErrors muted_errors)
{
    // 1. If muted errors was not provided, let it be false. (NOTE: This is taken care of by the default argument.)

    // 2. If muted errors is true, then set baseURL to about:blank.
    if (muted_errors == MutedErrors::Yes)
        base_url = "about:blank";

    // FIXME: 3. If scripting is disabled for settings, then set source to the empty string.

    // 4. Let script be a new classic script that this algorithm will subsequently initialize.
    auto script = adopt_ref(*new ClassicScript(move(base_url), move(filename), environment_settings_object));

    // 5. Set script's settings object to settings. (NOTE: This was already done when constructing.)

    // 6. Set script's base URL to baseURL. (NOTE: This was already done when constructing.)

    // FIXME: 7. Set script's fetch options to options.

    // 8. Set script's muted errors to muted errors.
    script->m_muted_errors = muted_errors;

    // FIXME: 9. Set script's parse error and error to rethrow to null.

    // 10. Let result be ParseScript(source, settings's Realm, script).
    auto parse_timer = Core::ElapsedTimer::start_new();
    auto result = JS::Script::parse(source, environment_settings_object.realm(), script->filename());
    dbgln("ClassicScript: Parsed {} in {}ms", script->filename(), parse_timer.elapsed());

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
    // FIXME: Remove this once this function is spec compliant.
    if (!m_script_record) {
        // FIXME: Throw a SyntaxError per the spec.
        dbgln("ClassicScript: Unable to run script {}", filename());
        return {};
    }

    // 1. Let settings be the settings object of script. (NOTE: Not necessary)

    // 2. Check if we can run script with settings.
    // FIXME: If this returns "do not run" then return NormalCompletion(empty).
    if (m_settings_object.can_run_script() == RunScriptDecision::DoNotRun)
        return {};

    // 3. Prepare to run script given settings.
    m_settings_object.prepare_to_run_script();

    // FIXME: 4. Let evaluationPromise be null.

    // FIXME: 5. If script's error to rethrow is not null, then set evaluationStatus to Completion { [[Type]]: throw, [[Value]]: script's error to rethrow, [[Target]]: empty }.

    dbgln("ClassicScript: Running script {}", filename());
    (void)rethrow_errors;

    auto timer = Core::ElapsedTimer::start_new();

    // 6. Otherwise, set evaluationStatus to ScriptEvaluation(script's record).
    // FIXME: Interpreter::run doesn't currently return a JS::Completion.
    auto interpreter = JS::Interpreter::create_with_existing_realm(m_script_record->realm());
    interpreter->run(*m_script_record);

    // FIXME: If ScriptEvaluation does not complete because the user agent has aborted the running script, leave evaluationStatus as null.

    auto& vm = interpreter->vm();

    // 7. If evaluationStatus is an abrupt completion, then:
    if (vm.exception()) {
        vm.clear_exception();

        // FIXME: 1. If rethrow errors is true and script's muted errors is false, then:
        //          1. Clean up after running script with settings.
        //          2. Rethrow evaluationStatus.[[Value]].

        // FIXME: 2. If rethrow errors is true and script's muted errors is true, then:
        //          1. Clean up after running script with settings.
        //          2. Throw a "NetworkError" DOMException.

        // FIXME: 3. Otherwise, rethrow errors is false. Perform the following steps:
        //          1. Report the exception given by evaluationStatus.[[Value]] for script.
        //          2. Clean up after running script with settings.
        //          3. Return evaluationStatus.
    }

    dbgln("ClassicScript: Finished running script {}, Duration: {}ms", filename(), timer.elapsed());

    // 8. Clean up after running script with settings.
    m_settings_object.clean_up_after_running_script();

    // FIXME: 9. If evaluationStatus is a normal completion, then return evaluationStatus.
    return vm.last_value();

    // FIXME: 10. If we've reached this point, evaluationStatus was left as null because the script was aborted prematurely during evaluation.
    //            Return Completion { [[Type]]: throw, [[Value]]: a new "QuotaExceededError" DOMException, [[Target]]: empty }.
}

ClassicScript::ClassicScript(AK::URL base_url, String filename, EnvironmentSettingsObject& environment_settings_object)
    : Script(move(base_url), move(filename))
    , m_settings_object(environment_settings_object)
{
}

ClassicScript::~ClassicScript()
{
}

}
