/*
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URLParser.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/Fetching.h>
#include <LibWeb/HTML/Scripting/ModuleScript.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/MimeSniff/MimeType.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#module-type-from-module-request
String module_type_from_module_request(JS::ModuleRequest const& module_request)
{
    // 1. Let moduleType be "javascript".
    String module_type = "javascript"sv;

    // 2. If moduleRequest.[[Assertions]] has a Record entry such that entry.[[Key]] is "type", then:
    for (auto const& entry : module_request.assertions) {
        if (entry.key != "type"sv)
            continue;

        // 1. If entry.[[Value]] is "javascript", then set moduleType to null.
        if (entry.value == "javascript"sv)
            module_type = nullptr;
        // 2. Otherwise, set moduleType to entry.[[Value]].
        else
            module_type = entry.value;
    }

    // 3. Return moduleType.
    return module_type;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#resolve-a-module-specifier
AK::URL resolve_module_specifier(JS::ModuleRequest const& module_request, AK::URL const& base_url)
{
    auto specifier = module_request.module_specifier;

    // 1. Apply the URL parser to specifier. If the result is not failure, return the result.
    auto result = URLParser::parse(specifier);
    if (result.is_valid())
        return result;

    // 2. If specifier does not start with the character U+002F SOLIDUS (/), the two-character sequence U+002E FULL STOP,
    //    U+002F SOLIDUS (./), or the three-character sequence U+002E FULL STOP, U+002E FULL STOP, U+002F SOLIDUS (../), return failure.
    if (!specifier.starts_with("/"sv) && !specifier.starts_with("./"sv) && !specifier.starts_with("../"sv))
        return {};

    // 3. Return the result of applying the URL parser to specifier with base URL.
    return URLParser::parse(specifier, &base_url);
}

// https://html.spec.whatwg.org/multipage/webappapis.html#internal-module-script-graph-fetching-procedure
void fetch_internal_module_script_graph(JS::ModuleRequest const& module_request, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, EnvironmentSettingsObject& module_script_settings_object, HashTable<ModuleLocationTuple> const& visited_set, AK::URL const& referrer, ModuleCallback on_complete)
{
    // 1. Let url be the result of resolving a module specifier given referrer and moduleRequest.[[Specifier]].
    auto url = resolve_module_specifier(module_request, referrer);

    // 2. Assert: url is never failure, because resolving a module specifier must have been previously successful with these same two arguments.
    VERIFY(url.is_valid());

    // 3. Let moduleType be the result of running the module type from module request steps given moduleRequest.
    auto module_type = module_type_from_module_request(module_request);

    // 4. Assert: visited set contains (url, moduleType).
    VERIFY(visited_set.contains({ url, module_type }));

    // 5. Fetch a single module script given url, fetch client settings object, destination, options,module map settings object, referrer,
    //    moduleRequest, false, and onSingleFetchComplete as defined below. If performFetch was given, pass it along as well.
    // FIXME: Pass options.
    fetch_single_module_script(url, fetch_client_settings_object, destination, module_script_settings_object, referrer, module_request, TopLevelModule::No, [on_complete = move(on_complete), &fetch_client_settings_object, destination, visited_set](auto const* result) mutable {
        // onSingleFetchComplete given result is the following algorithm:
        // 1. If result is null, run onComplete with null, and abort these steps.
        if (!result) {
            on_complete(nullptr);
            return;
        }

        // 2. Fetch the descendants of result given fetch client settings object, destination, visited set, and with onComplete. If performFetch was given, pass it along as well.
        // FIXME: Pass performFetch if given.
        fetch_descendants_of_a_module_script(*result, fetch_client_settings_object, destination, visited_set, move(on_complete));
    });
}

// https://html.spec.whatwg.org/multipage/webappapis.html#fetch-the-descendants-of-a-module-script
void fetch_descendants_of_a_module_script(JavaScriptModuleScript const& module_script, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, HashTable<ModuleLocationTuple> visited_set, ModuleCallback on_complete)
{
    // 1. If module script's record is null, run onComplete with module script and return.
    if (!module_script.record()) {
        on_complete(&module_script);
        return;
    }

    // 2. Let record be module script's record.
    auto const& record = module_script.record();

    // 3. If record is not a Cyclic Module Record, or if record.[[RequestedModules]] is empty, run onComplete with module script and return.
    // FIXME: Currently record is always a cyclic module.
    if (record->requested_modules().is_empty()) {
        on_complete(&module_script);
        return;
    }

    // 4. Let moduleRequests be a new empty list.
    Vector<JS::ModuleRequest> module_requests;

    // 5. For each ModuleRequest Record requested of record.[[RequestedModules]],
    for (auto const& requested : record->requested_modules()) {
        // 1. Let url be the result of resolving a module specifier given module script's base URL and requested.[[Specifier]].
        auto url = resolve_module_specifier(requested, module_script.base_url());

        // 2. Assert: url is never failure, because resolving a module specifier must have been previously successful with these same two arguments.
        VERIFY(url.is_valid());

        // 3. Let moduleType be the result of running the module type from module request steps given requested.
        auto module_type = module_type_from_module_request(requested);

        // 4. If visited set does not contain (url, moduleType), then:
        if (!visited_set.contains({ url, module_type })) {
            // 1. Append requested to moduleRequests.
            module_requests.append(requested);

            // 2. Append (url, moduleType) to visited set.
            visited_set.set({ url, module_type });
        }
    }

    // FIXME: 6. Let options be the descendant script fetch options for module script's fetch options.

    // FIXME: 7. Assert: options is not null, as module script is a JavaScript module script.

    // 8. Let pendingCount be the length of moduleRequests.
    auto pending_count = module_requests.size();

    // 9. If pendingCount is zero, run onComplete with module script.
    if (pending_count == 0) {
        on_complete(&module_script);
        return;
    }

    // 10. Let failed be false.
    auto context = DescendantFetchingContext::create();
    context->set_pending_count(pending_count);
    context->set_failed(false);
    context->set_on_complete(move(on_complete));

    // 11. For each moduleRequest in moduleRequests, perform the internal module script graph fetching procedure given moduleRequest,
    //     fetch client settings object, destination, options, module script's settings object, visited set, module script's base URL,
    //     and onInternalFetchingComplete as defined below. If performFetch was given, pass it along as well.
    for (auto const& module_request : module_requests) {
        // FIXME: Pass options and performFetch if given.
        fetch_internal_module_script_graph(module_request, fetch_client_settings_object, destination, const_cast<JavaScriptModuleScript&>(module_script).settings_object(), visited_set, module_script.base_url(), [context, &module_script](auto const* result) mutable {
            // onInternalFetchingComplete given result is the following algorithm:
            // 1. If failed is true, then abort these steps.
            if (context->failed())
                return;

            // 2. If result is null, then set failed to true, run onComplete with null, and abort these steps.
            if (!result) {
                context->set_failed(true);
                context->on_complete(nullptr);
                return;
            }

            // 3. Assert: pendingCount is greater than zero.
            VERIFY(context->pending_count() > 0);

            // 4. Decrement pendingCount by one.
            context->decrement_pending_count();

            // 5. If pendingCount is zero, run onComplete with module script.
            if (context->pending_count() == 0)
                context->on_complete(&module_script);
        });
    }
}

// https://html.spec.whatwg.org/multipage/webappapis.html#fetch-a-single-module-script
void fetch_single_module_script(AK::URL const& url, EnvironmentSettingsObject&, StringView, EnvironmentSettingsObject& module_map_settings_object, AK::URL const&, Optional<JS::ModuleRequest> const& module_request, TopLevelModule, ModuleCallback on_complete)
{
    // 1. Let moduleType be "javascript".
    String module_type = "javascript"sv;

    // 2. If moduleRequest was given, then set moduleType to the result of running the module type from module request steps given moduleRequest.
    if (module_request.has_value())
        module_type = module_type_from_module_request(*module_request);

    // 3. Assert: the result of running the module type allowed steps given moduleType and module map settings object is true.
    //    Otherwise we would not have reached this point because a failure would have been raised when inspecting moduleRequest.[[Assertions]]
    //    in create a JavaScript module script or fetch an import() module script graph.
    VERIFY(module_map_settings_object.module_type_allowed(module_type));

    // 4. Let moduleMap be module map settings object's module map.
    auto& module_map = module_map_settings_object.module_map();

    // 5. If moduleMap[(url, moduleType)] is "fetching", wait in parallel until that entry's value changes,
    //    then queue a task on the networking task source to proceed with running the following steps.
    if (module_map.is_fetching(url, module_type)) {
        module_map.wait_for_change(url, module_type, [on_complete = move(on_complete)](auto entry) {
            // FIXME: This should queue a task.

            // FIXME: This should run other steps, for now we just assume the script loaded.
            VERIFY(entry.type == ModuleMap::EntryType::ModuleScript);

            on_complete(entry.module_script);
        });

        return;
    }

    // 6. If moduleMap[(url, moduleType)] exists, run onComplete given moduleMap[(url, moduleType)], and return.
    auto entry = module_map.get(url, module_type);
    if (entry.has_value() && entry->type == ModuleMap::EntryType::ModuleScript) {
        on_complete(entry->module_script);
        return;
    }

    // 7. Set moduleMap[(url, moduleType)] to "fetching".
    module_map.set(url, module_type, { ModuleMap::EntryType::Fetching, nullptr });

    // FIXME: Implement non ad-hoc version of steps 8 to 20.

    auto request = LoadRequest::create_for_url_on_page(url, nullptr);

    ResourceLoader::the().load(
        request,
        [url, module_type, &module_map_settings_object, on_complete = move(on_complete), &module_map](StringView data, auto& response_headers, auto) {
            if (data.is_null()) {
                dbgln("Failed to load module {}", url);
                module_map.set(url, module_type, { ModuleMap::EntryType::Failed, nullptr });
                on_complete(nullptr);
                return;
            }

            auto content_type_header = response_headers.get("Content-Type");
            if (!content_type_header.has_value()) {
                dbgln("Module has no content type! {}", url);
                module_map.set(url, module_type, { ModuleMap::EntryType::Failed, nullptr });
                on_complete(nullptr);
                return;
            }

            if (MimeSniff::is_javascript_mime_type_essence_match(*content_type_header) && module_type == "javascript"sv) {
                auto module_script = JavaScriptModuleScript::create(url.basename(), data, module_map_settings_object, url);
                module_map.set(url, module_type, { ModuleMap::EntryType::ModuleScript, module_script });
                on_complete(module_script);
                return;
            }

            dbgln("Module has no JS content type! {} of type {}, with content {}", url, module_type, *content_type_header);
            module_map.set(url, module_type, { ModuleMap::EntryType::Failed, nullptr });
            on_complete(nullptr);
        },
        [module_type, url](auto&, auto) {
            dbgln("Failed to load module script");
            TODO();
        });
}

// https://html.spec.whatwg.org/multipage/webappapis.html#fetch-a-module-script-tree
void fetch_external_module_script_graph(AK::URL const& url, EnvironmentSettingsObject& settings_object, ModuleCallback on_complete)
{
    // 1. Fetch a single module script given url, settings object, "script", options, settings object, "client", true, and with the following steps given result:
    // FIXME: Pass options.
    fetch_single_module_script(url, settings_object, "script"sv, settings_object, "client"sv, {}, TopLevelModule::Yes, [&settings_object, on_complete = move(on_complete), &url](auto* result) mutable {
        // 1. If result is null, run onComplete given null, and abort these steps.
        if (!result) {
            on_complete(nullptr);
            return;
        }

        // 2. Let visited set be « (url, "javascript") ».
        HashTable<ModuleLocationTuple> visited_set;
        visited_set.set({ url, "javascript"sv });

        // 3. Fetch the descendants of and link result given settings object, "script", visited set, and onComplete.
        fetch_descendants_of_and_link_a_module_script(*result, settings_object, "script"sv, move(visited_set), move(on_complete));
    });
}

// https://html.spec.whatwg.org/multipage/webappapis.html#fetch-an-inline-module-script-graph
void fetch_inline_module_script_graph(String const& filename, String const& source_text, AK::URL const& base_url, EnvironmentSettingsObject& settings_object, ModuleCallback on_complete)
{
    // 1. Let script be the result of creating a JavaScript module script using source text, settings object, base URL, and options.
    auto script = JavaScriptModuleScript::create(filename, source_text.view(), settings_object, base_url);

    // 2. If script is null, run onComplete given null, and return.
    if (!script) {
        on_complete(nullptr);
        return;
    }

    // 3. Let visited set be an empty set.
    HashTable<ModuleLocationTuple> visited_set;

    // 4. Fetch the descendants of and link script, given settings object, the destination "script", visited set, and onComplete.
    fetch_descendants_of_and_link_a_module_script(*script, settings_object, "script"sv, visited_set, move(on_complete));
}

// https://html.spec.whatwg.org/multipage/webappapis.html#fetch-the-descendants-of-and-link-a-module-script
void fetch_descendants_of_and_link_a_module_script(JavaScriptModuleScript const& module_script, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, HashTable<ModuleLocationTuple> const& visited_set, ModuleCallback on_complete)
{
    // 1. Fetch the descendants of module script, given fetch client settings object, destination, visited set, and onFetchDescendantsComplete as defined below.
    //    If performFetch was given, pass it along as well.
    // FIXME: Pass performFetch if given.
    fetch_descendants_of_a_module_script(module_script, fetch_client_settings_object, destination, visited_set, [&fetch_client_settings_object, on_complete = move(on_complete)](JavaScriptModuleScript const* result) {
        // onFetchDescendantsComplete given result is the following algorithm:
        // 1. If result is null, then run onComplete given result, and abort these steps.
        if (!result) {
            on_complete(nullptr);
            return;
        }

        // FIXME: 2. Let parse error be the result of finding the first parse error given result.

        // 3. If parse error is null, then:
        if (result->record()) {
            // 1. Let record be result's record.
            auto const& record = *result->record();

            // NOTE: Although the spec does not say this we have to have a proper execution context when linking so we modify the stack here.
            // FIXME: Determine whether we are missing something from the spec or the spec is missing this step.
            fetch_client_settings_object.realm().vm().push_execution_context(fetch_client_settings_object.realm_execution_context());

            // 2. Perform record.Link().
            auto linking_result = const_cast<JS::SourceTextModule&>(record).link(result->vm());

            fetch_client_settings_object.realm().vm().pop_execution_context();

            // TODO: If this throws an exception, set result's error to rethrow to that exception.
            if (linking_result.is_error())
                TODO();
        } else {
            // FIXME: 4. Otherwise, set result's error to rethrow to parse error.
            TODO();
        }

        // 5. Run onComplete given result.
        on_complete(result);
    });
}

}
