/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/ElapsedTimer.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/WebIDL/DOMException.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#creating-a-classic-script
JS::NonnullGCPtr<ClassicScript> ClassicScript::create(DeprecatedString filename, StringView source, EnvironmentSettingsObject& environment_settings_object, AK::URL base_url, size_t source_line_number, MutedErrors muted_errors)
{
    auto& vm = environment_settings_object.realm().vm();

    // 1. If muted errors was not provided, let it be false. (NOTE: This is taken care of by the default argument.)

    // 2. If muted errors is true, then set baseURL to about:blank.
    if (muted_errors == MutedErrors::Yes)
        base_url = "about:blank"sv;

    // 3. If scripting is disabled for settings, then set source to the empty string.
    if (environment_settings_object.is_scripting_disabled())
        source = ""sv;

    // 4. Let script be a new classic script that this algorithm will subsequently initialize.
    auto script = vm.heap().allocate_without_realm<ClassicScript>(move(base_url), move(filename), environment_settings_object);

    // 5. Set script's settings object to settings. (NOTE: This was already done when constructing.)

    // 6. Set script's base URL to baseURL. (NOTE: This was already done when constructing.)

    // FIXME: 7. Set script's fetch options to options.

    // 8. Set script's muted errors to muted errors.
    script->m_muted_errors = muted_errors;

    // 9. Set script's parse error and error to rethrow to null.
    script->set_parse_error(JS::js_null());
    script->set_error_to_rethrow(JS::js_null());

    // 10. Let result be ParseScript(source, settings's Realm, script).
    auto parse_timer = Core::ElapsedTimer::start_new();
    auto result = JS::Script::parse(source, environment_settings_object.realm(), script->filename(), script, source_line_number);
    dbgln_if(HTML_SCRIPT_DEBUG, "ClassicScript: Parsed {} in {}ms", script->filename(), parse_timer.elapsed());

    // 11. If result is a list of errors, then:
    if (result.is_error()) {
        auto& parse_error = result.error().first();
        dbgln_if(HTML_SCRIPT_DEBUG, "ClassicScript: Failed to parse: {}", parse_error.to_deprecated_string());

        // 1. Set script's parse error and its error to rethrow to result[0].
        script->set_parse_error(JS::SyntaxError::create(environment_settings_object.realm(), parse_error.to_string().release_value_but_fixme_should_propagate_errors()));
        script->set_error_to_rethrow(script->parse_error());

        // 2. Return script.
        return script;
    }

    // 12. Set script's record to result.
    script->m_script_record = *result.release_value();

    // 13. Return script.
    return script;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#run-a-classic-script
JS::Completion ClassicScript::run(RethrowErrors rethrow_errors, JS::GCPtr<JS::Environment> lexical_environment_override)
{
    // 1. Let settings be the settings object of script.
    auto& settings = settings_object();

    // 2. Check if we can run script with settings. If this returns "do not run" then return NormalCompletion(empty).
    if (settings.can_run_script() == RunScriptDecision::DoNotRun)
        return JS::normal_completion({});

    // 3. Prepare to run script given settings.
    settings.prepare_to_run_script();

    // 4. Let evaluationStatus be null.
    JS::Completion evaluation_status;

    // 5. If script's error to rethrow is not null, then set evaluationStatus to Completion { [[Type]]: throw, [[Value]]: script's error to rethrow, [[Target]]: empty }.
    if (!error_to_rethrow().is_null()) {
        evaluation_status = JS::Completion { JS::Completion::Type::Throw, error_to_rethrow(), {} };
    } else {
        auto timer = Core::ElapsedTimer::start_new();

        // 6. Otherwise, set evaluationStatus to ScriptEvaluation(script's record).
        evaluation_status = vm().bytecode_interpreter().run(*m_script_record, lexical_environment_override);

        // FIXME: If ScriptEvaluation does not complete because the user agent has aborted the running script, leave evaluationStatus as null.

        dbgln_if(HTML_SCRIPT_DEBUG, "ClassicScript: Finished running script {}, Duration: {}ms", filename(), timer.elapsed());
    }

    // 7. If evaluationStatus is an abrupt completion, then:
    if (evaluation_status.is_abrupt()) {
        // 1. If rethrow errors is true and script's muted errors is false, then:
        if (rethrow_errors == RethrowErrors::Yes && m_muted_errors == MutedErrors::No) {
            // 1. Clean up after running script with settings.
            settings.clean_up_after_running_script();

            // 2. Rethrow evaluationStatus.[[Value]].
            return JS::throw_completion(*evaluation_status.value());
        }

        // 2. If rethrow errors is true and script's muted errors is true, then:
        if (rethrow_errors == RethrowErrors::Yes && m_muted_errors == MutedErrors::Yes) {
            // 1. Clean up after running script with settings.
            settings.clean_up_after_running_script();

            // 2. Throw a "NetworkError" DOMException.
            return throw_completion(WebIDL::NetworkError::create(settings.realm(), "Script error."_fly_string));
        }

        // 3. Otherwise, rethrow errors is false. Perform the following steps:
        VERIFY(rethrow_errors == RethrowErrors::No);

        // 1. Report the exception given by evaluationStatus.[[Value]] for script.
        report_exception(evaluation_status, settings_object().realm());

        // 2. Clean up after running script with settings.
        settings.clean_up_after_running_script();

        // 3. Return evaluationStatus.
        return evaluation_status;
    }

    // 8. Clean up after running script with settings.
    settings.clean_up_after_running_script();

    // 9. If evaluationStatus is a normal completion, then return evaluationStatus.
    VERIFY(!evaluation_status.is_abrupt());
    return evaluation_status;

    // FIXME: 10. If we've reached this point, evaluationStatus was left as null because the script was aborted prematurely during evaluation.
    //            Return Completion { [[Type]]: throw, [[Value]]: a new "QuotaExceededError" DOMException, [[Target]]: empty }.
}

ClassicScript::ClassicScript(AK::URL base_url, DeprecatedString filename, EnvironmentSettingsObject& environment_settings_object)
    : Script(move(base_url), move(filename), environment_settings_object)
{
}

ClassicScript::~ClassicScript() = default;

void ClassicScript::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_script_record);
}

}
