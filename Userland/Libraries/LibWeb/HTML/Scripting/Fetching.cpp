/*
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ModuleRequest.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Fetch/Fetching/Fetching.h>
#include <LibWeb/Fetch/Infrastructure/FetchAlgorithms.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Headers.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/PotentialCORSRequest.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/Fetching.h>
#include <LibWeb/HTML/Scripting/ModuleScript.h>
#include <LibWeb/HTML/Scripting/TemporaryExecutionContext.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/MimeSniff/MimeType.h>
#include <LibWeb/URL/URL.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#module-type-from-module-request
DeprecatedString module_type_from_module_request(JS::ModuleRequest const& module_request)
{
    // 1. Let moduleType be "javascript".
    DeprecatedString module_type = "javascript"sv;

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
WebIDL::ExceptionOr<AK::URL> resolve_module_specifier(Optional<Script&> referring_script, DeprecatedString const& specifier)
{
    // 1. Let settingsObject and baseURL be null.
    Optional<EnvironmentSettingsObject&> settings_object;
    Optional<AK::URL const&> base_url;

    // 2. If referringScript is not null, then:
    if (referring_script.has_value()) {
        // 1. Set settingsObject to referringScript's settings object.
        settings_object = referring_script->settings_object();

        // 2. Set baseURL to referringScript's base URL.
        base_url = referring_script->base_url();
    }
    // 3. Otherwise:
    else {
        // 1. Assert: there is a current settings object.
        // NOTE: This is handled by the current_settings_object() accessor.

        // 2. Set settingsObject to the current settings object.
        settings_object = current_settings_object();

        // 3. Set baseURL to settingsObject's API base URL.
        base_url = settings_object->api_base_url();
    }

    // 4. Let importMap be an empty import map.
    ImportMap import_map;

    // 5. If settingsObject's global object implements Window, then set importMap to settingsObject's global object's import map.
    if (is<Window>(settings_object->global_object()))
        import_map = verify_cast<Window>(settings_object->global_object()).import_map();

    // 6. Let baseURLString be baseURL, serialized.
    auto base_url_string = base_url->serialize();

    // 7. Let asURL be the result of resolving a URL-like module specifier given specifier and baseURL.
    auto as_url = resolve_url_like_module_specifier(specifier, *base_url);

    // 8. Let normalizedSpecifier be the serialization of asURL, if asURL is non-null; otherwise, specifier.
    auto normalized_specifier = as_url.has_value() ? as_url->serialize() : specifier;

    // 9. For each scopePrefix → scopeImports of importMap's scopes:
    for (auto const& entry : import_map.scopes()) {
        // FIXME: Clarify if the serialization steps need to be run here. The steps below assume
        //        scopePrefix to be a string.
        auto const& scope_prefix = entry.key.serialize();
        auto const& scope_imports = entry.value;

        // 1. If scopePrefix is baseURLString, or if scopePrefix ends with U+002F (/) and scopePrefix is a code unit prefix of baseURLString, then:
        if (scope_prefix == base_url_string || (scope_prefix.ends_with("/"sv) && Infra::is_code_unit_prefix(scope_prefix, base_url_string))) {
            // 1. Let scopeImportsMatch be the result of resolving an imports match given normalizedSpecifier, asURL, and scopeImports.
            auto scope_imports_match = TRY(resolve_imports_match(normalized_specifier, as_url, scope_imports));

            // 2. If scopeImportsMatch is not null, then return scopeImportsMatch.
            if (scope_imports_match.has_value())
                return scope_imports_match.release_value();
        }
    }

    // 10. Let topLevelImportsMatch be the result of resolving an imports match given normalizedSpecifier, asURL, and importMap's imports.
    auto top_level_imports_match = TRY(resolve_imports_match(normalized_specifier, as_url, import_map.imports()));

    // 11. If topLevelImportsMatch is not null, then return topLevelImportsMatch.
    if (top_level_imports_match.has_value())
        return top_level_imports_match.release_value();

    // 12. If asURL is not null, then return asURL.
    if (as_url.has_value())
        return as_url.release_value();

    // 13. Throw a TypeError indicating that specifier was a bare specifier, but was not remapped to anything by importMap.
    return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, String::formatted("Failed to resolve non relative module specifier '{}' from an import map.", specifier).release_value_but_fixme_should_propagate_errors() };
}

// https://html.spec.whatwg.org/multipage/webappapis.html#resolving-an-imports-match
WebIDL::ExceptionOr<Optional<AK::URL>> resolve_imports_match(DeprecatedString const& normalized_specifier, Optional<AK::URL> as_url, ModuleSpecifierMap const& specifier_map)
{
    // 1. For each specifierKey → resolutionResult of specifierMap:
    for (auto const& [specifier_key, resolution_result] : specifier_map) {
        // 1. If specifierKey is normalizedSpecifier, then:
        if (specifier_key == normalized_specifier) {
            // 1. If resolutionResult is null, then throw a TypeError indicating that resolution of specifierKey was blocked by a null entry.
            if (!resolution_result.has_value())
                return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, String::formatted("Import resolution of '{}' was blocked by a null entry.", specifier_key).release_value_but_fixme_should_propagate_errors() };

            // 2. Assert: resolutionResult is a URL.
            VERIFY(resolution_result->is_valid());

            // 3. Return resolutionResult.
            return resolution_result;
        }

        // 2. If all of the following are true:
        if (
            // - specifierKey ends with U+002F (/);
            specifier_key.ends_with("/"sv) &&
            // - specifierKey is a code unit prefix of normalizedSpecifier; and
            Infra::is_code_unit_prefix(specifier_key, normalized_specifier) &&
            // - either asURL is null, or asURL is special,
            (!as_url.has_value() || as_url->is_special())
            // then:
        ) {
            // 1. If resolutionResult is null, then throw a TypeError indicating that the resolution of specifierKey was blocked by a null entry.
            if (!resolution_result.has_value())
                return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, String::formatted("Import resolution of '{}' was blocked by a null entry.", specifier_key).release_value_but_fixme_should_propagate_errors() };

            // 2. Assert: resolutionResult is a URL.
            VERIFY(resolution_result->is_valid());

            // 3. Let afterPrefix be the portion of normalizedSpecifier after the initial specifierKey prefix.
            // FIXME: Clarify if this is meant by the portion after the initial specifierKey prefix.
            auto after_prefix = normalized_specifier.substring(specifier_key.length());

            // 4. Assert: resolutionResult, serialized, ends with U+002F (/), as enforced during parsing.
            VERIFY(resolution_result->serialize().ends_with("/"sv));

            // 5. Let url be the result of URL parsing afterPrefix with resolutionResult.
            auto url = URL::parse(after_prefix, *resolution_result);

            // 6. If url is failure, then throw a TypeError indicating that resolution of normalizedSpecifier was blocked since the afterPrefix portion
            //    could not be URL-parsed relative to the resolutionResult mapped to by the specifierKey prefix.
            if (!url.is_valid())
                return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, String::formatted("Could not resolve '{}' as the after prefix portion could not be URL-parsed.", normalized_specifier).release_value_but_fixme_should_propagate_errors() };

            // 7. Assert: url is a URL.
            VERIFY(url.is_valid());

            // 8. If the serialization of resolutionResult is not a code unit prefix of the serialization of url, then throw a TypeError indicating
            //    that the resolution of normalizedSpecifier was blocked due to it backtracking above its prefix specifierKey.
            if (!Infra::is_code_unit_prefix(resolution_result->serialize(), url.serialize()))
                return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, String::formatted("Could not resolve '{}' as it backtracks above its prefix specifierKey.", normalized_specifier).release_value_but_fixme_should_propagate_errors() };

            // 9. Return url.
            return url;
        }
    }

    // 2. Return null.
    return Optional<AK::URL> {};
}

// https://html.spec.whatwg.org/multipage/webappapis.html#resolving-a-url-like-module-specifier
Optional<AK::URL> resolve_url_like_module_specifier(DeprecatedString const& specifier, AK::URL const& base_url)
{
    // 1. If specifier starts with "/", "./", or "../", then:
    if (specifier.starts_with("/"sv) || specifier.starts_with("./"sv) || specifier.starts_with("../"sv)) {
        // 1. Let url be the result of URL parsing specifier with baseURL.
        auto url = URL::parse(specifier, base_url);

        // 2. If url is failure, then return null.
        if (!url.is_valid())
            return {};

        // 3. Return url.
        return url;
    }

    // 2. Let url be the result of URL parsing specifier (with no base URL).
    auto url = URL::parse(specifier);

    // 3. If url is failure, then return null.
    if (!url.is_valid())
        return {};

    // 4. Return url.
    return url;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#set-up-the-classic-script-request
static void set_up_classic_script_request(Fetch::Infrastructure::Request& request, ScriptFetchOptions const& options)
{
    // Set request's cryptographic nonce metadata to options's cryptographic nonce, its integrity metadata to options's
    // integrity metadata, its parser metadata to options's parser metadata, its referrer policy to options's referrer
    // policy, its render-blocking to options's render-blocking, and its priority to options's fetch priority.
    request.set_cryptographic_nonce_metadata(options.cryptographic_nonce);
    request.set_integrity_metadata(options.integrity_metadata);
    request.set_parser_metadata(options.parser_metadata);
    request.set_referrer_policy(options.referrer_policy);
    request.set_render_blocking(options.render_blocking);
    request.set_priority(options.fetch_priority);
}

// https://html.spec.whatwg.org/multipage/webappapis.html#fetch-a-classic-script
WebIDL::ExceptionOr<void> fetch_classic_script(JS::NonnullGCPtr<HTMLScriptElement> element, AK::URL const& url, EnvironmentSettingsObject& settings_object, ScriptFetchOptions options, CORSSettingAttribute cors_setting, String character_encoding, OnFetchScriptComplete on_complete)
{
    auto& realm = element->realm();
    auto& vm = realm.vm();

    // 1. Let request be the result of creating a potential-CORS request given url, "script", and CORS setting.
    auto request = create_potential_CORS_request(vm, url, Fetch::Infrastructure::Request::Destination::Script, cors_setting);

    // 2. Set request's client to settings object.
    request->set_client(&settings_object);

    // 3. Set request's initiator type to "script".
    request->set_initiator_type(Fetch::Infrastructure::Request::InitiatorType::Script);

    // 4. Set up the classic script request given request and options.
    set_up_classic_script_request(*request, options);

    // 5. Fetch request with the following processResponseConsumeBody steps given response response and null, failure,
    //    or a byte sequence bodyBytes:
    Fetch::Infrastructure::FetchAlgorithms::Input fetch_algorithms_input {};
    fetch_algorithms_input.process_response_consume_body = [&settings_object, options = move(options), character_encoding = move(character_encoding), on_complete = move(on_complete)](auto response, auto body_bytes) {
        // 1. Set response to response's unsafe response.
        response = response->unsafe_response();

        // 2. If either of the following conditions are met:
        // - bodyBytes is null or failure; or
        // - response's status is not an ok status,
        if (body_bytes.template has<Empty>() || body_bytes.template has<Fetch::Infrastructure::FetchAlgorithms::ConsumeBodyFailureTag>() || !Fetch::Infrastructure::is_ok_status(response->status())) {
            // then run onComplete given null, and abort these steps.
            on_complete(nullptr);
            return;
        }

        // 3. Let potentialMIMETypeForEncoding be the result of extracting a MIME type given response's header list.
        auto potential_mime_type_for_encoding = response->header_list()->extract_mime_type().release_value_but_fixme_should_propagate_errors();

        // 4. Set character encoding to the result of legacy extracting an encoding given potentialMIMETypeForEncoding
        //    and character encoding.
        auto extracted_character_encoding = Fetch::Infrastructure::legacy_extract_an_encoding(potential_mime_type_for_encoding, character_encoding);

        // 5. Let source text be the result of decoding bodyBytes to Unicode, using character encoding as the fallback
        //    encoding.
        auto fallback_decoder = TextCodec::decoder_for(extracted_character_encoding);
        VERIFY(fallback_decoder.has_value());

        auto source_text = TextCodec::convert_input_to_utf8_using_given_decoder_unless_there_is_a_byte_order_mark(*fallback_decoder, body_bytes.template get<ByteBuffer>()).release_value_but_fixme_should_propagate_errors();

        // 6. Let muted errors be true if response was CORS-cross-origin, and false otherwise.
        auto muted_errors = response->is_cors_cross_origin() ? ClassicScript::MutedErrors::Yes : ClassicScript::MutedErrors::No;

        // 7. Let script be the result of creating a classic script given source text, settings object, response's URL,
        //    options, and muted errors.
        // FIXME: Pass options.
        auto response_url = response->url().value_or({});
        auto script = ClassicScript::create(response_url.to_deprecated_string(), source_text, settings_object, response_url, 1, muted_errors);

        // 8. Run onComplete given script.
        on_complete(script);
    };

    TRY(Fetch::Fetching::fetch(element->realm(), request, Fetch::Infrastructure::FetchAlgorithms::create(vm, move(fetch_algorithms_input))));
    return {};
}

// https://html.spec.whatwg.org/multipage/webappapis.html#internal-module-script-graph-fetching-procedure
void fetch_internal_module_script_graph(JS::ModuleRequest const& module_request, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, Script& referring_script, HashTable<ModuleLocationTuple> const& visited_set, OnFetchScriptComplete on_complete)
{
    // 1. Let url be the result of resolving a module specifier given referringScript and moduleRequest.[[Specifier]].
    auto url = MUST(resolve_module_specifier(referring_script, module_request.module_specifier));

    // 2. Assert: the previous step never throws an exception, because resolving a module specifier must have been previously successful with these same two arguments.
    // NOTE: Handled by MUST above.

    // 3. Let moduleType be the result of running the module type from module request steps given moduleRequest.
    auto module_type = module_type_from_module_request(module_request);

    // 4. Assert: visited set contains (url, moduleType).
    VERIFY(visited_set.contains({ url, module_type }));

    // 5. Fetch a single module script given url, fetch client settings object, destination, options, referringScript's settings object,
    //    referringScript's base URL, moduleRequest, false, and onSingleFetchComplete as defined below. If performFetch was given, pass it along as well.
    // FIXME: Pass options and performFetch if given.
    fetch_single_module_script(url, fetch_client_settings_object, destination, referring_script.settings_object(), referring_script.base_url(), module_request, TopLevelModule::No, [on_complete = move(on_complete), &fetch_client_settings_object, destination, visited_set](auto result) mutable {
        // onSingleFetchComplete given result is the following algorithm:
        // 1. If result is null, run onComplete with null, and abort these steps.
        if (!result) {
            on_complete(nullptr);
            return;
        }

        // 2. Fetch the descendants of result given fetch client settings object, destination, visited set, and with onComplete. If performFetch was given, pass it along as well.
        // FIXME: Pass performFetch if given.
        auto& module_script = verify_cast<JavaScriptModuleScript>(*result);
        fetch_descendants_of_a_module_script(module_script, fetch_client_settings_object, destination, visited_set, move(on_complete));
    });
}

// https://html.spec.whatwg.org/multipage/webappapis.html#fetch-the-descendants-of-a-module-script
void fetch_descendants_of_a_module_script(JavaScriptModuleScript& module_script, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, HashTable<ModuleLocationTuple> visited_set, OnFetchScriptComplete on_complete)
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
        // 1. Let url be the result of resolving a module specifier given module script and requested.[[Specifier]].
        auto url = MUST(resolve_module_specifier(module_script, requested.module_specifier));

        // 2. Assert: the previous step never throws an exception, because resolving a module specifier must have been previously successful with these same two arguments.
        // NOTE: Handled by MUST above.

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
    //     fetch client settings object, destination, options, module script, visited set, and onInternalFetchingComplete as defined below.
    //     If performFetch was given, pass it along as well.
    for (auto const& module_request : module_requests) {
        // FIXME: Pass options and performFetch if given.
        fetch_internal_module_script_graph(module_request, fetch_client_settings_object, destination, module_script, visited_set, [context, &module_script](auto result) mutable {
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
void fetch_single_module_script(AK::URL const& url, EnvironmentSettingsObject&, StringView, EnvironmentSettingsObject& module_map_settings_object, AK::URL const&, Optional<JS::ModuleRequest> const& module_request, TopLevelModule, OnFetchScriptComplete on_complete)
{
    // 1. Let moduleType be "javascript".
    DeprecatedString module_type = "javascript"sv;

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
                auto module_script = JavaScriptModuleScript::create(url.basename(), data, module_map_settings_object, url).release_value_but_fixme_should_propagate_errors();
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
void fetch_external_module_script_graph(AK::URL const& url, EnvironmentSettingsObject& settings_object, OnFetchScriptComplete on_complete)
{
    // 1. Disallow further import maps given settings object.
    settings_object.disallow_further_import_maps();

    // 2. Fetch a single module script given url, settings object, "script", options, settings object, "client", true, and with the following steps given result:
    // FIXME: Pass options.
    fetch_single_module_script(url, settings_object, "script"sv, settings_object, "client"sv, {}, TopLevelModule::Yes, [&settings_object, on_complete = move(on_complete), url](auto result) mutable {
        // 1. If result is null, run onComplete given null, and abort these steps.
        if (!result) {
            on_complete(nullptr);
            return;
        }

        // 2. Let visited set be « (url, "javascript") ».
        HashTable<ModuleLocationTuple> visited_set;
        visited_set.set({ url, "javascript"sv });

        // 3. Fetch the descendants of and link result given settings object, "script", visited set, and onComplete.
        auto& module_script = verify_cast<JavaScriptModuleScript>(*result);
        fetch_descendants_of_and_link_a_module_script(module_script, settings_object, "script"sv, move(visited_set), move(on_complete));
    });
}

// https://html.spec.whatwg.org/multipage/webappapis.html#fetch-an-inline-module-script-graph
void fetch_inline_module_script_graph(DeprecatedString const& filename, DeprecatedString const& source_text, AK::URL const& base_url, EnvironmentSettingsObject& settings_object, OnFetchScriptComplete on_complete)
{
    // 1. Disallow further import maps given settings object.
    settings_object.disallow_further_import_maps();

    // 2. Let script be the result of creating a JavaScript module script using source text, settings object, base URL, and options.
    auto script = JavaScriptModuleScript::create(filename, source_text.view(), settings_object, base_url).release_value_but_fixme_should_propagate_errors();

    // 3. If script is null, run onComplete given null, and return.
    if (!script) {
        on_complete(nullptr);
        return;
    }

    // 4. Let visited set be an empty set.
    HashTable<ModuleLocationTuple> visited_set;

    // 5. Fetch the descendants of and link script, given settings object, the destination "script", visited set, and onComplete.
    fetch_descendants_of_and_link_a_module_script(*script, settings_object, "script"sv, visited_set, move(on_complete));
}

// https://html.spec.whatwg.org/multipage/webappapis.html#fetch-the-descendants-of-and-link-a-module-script
void fetch_descendants_of_and_link_a_module_script(JavaScriptModuleScript& module_script, EnvironmentSettingsObject& fetch_client_settings_object, StringView destination, HashTable<ModuleLocationTuple> const& visited_set, OnFetchScriptComplete on_complete)
{
    // 1. Fetch the descendants of module script, given fetch client settings object, destination, visited set, and onFetchDescendantsComplete as defined below.
    //    If performFetch was given, pass it along as well.
    // FIXME: Pass performFetch if given.
    fetch_descendants_of_a_module_script(module_script, fetch_client_settings_object, destination, visited_set, [&fetch_client_settings_object, on_complete = move(on_complete)](auto result) {
        // onFetchDescendantsComplete given result is the following algorithm:
        // 1. If result is null, then run onComplete given result, and abort these steps.
        if (!result) {
            on_complete(nullptr);
            return;
        }

        TemporaryExecutionContext execution_context { fetch_client_settings_object };

        // FIXME: 2. Let parse error be the result of finding the first parse error given result.

        // 3. If parse error is null, then:
        if (auto& module_script = verify_cast<JavaScriptModuleScript>(*result); module_script.record()) {
            // 1. Let record be result's record.
            auto const& record = *module_script.record();

            // 2. Perform record.Link().
            auto linking_result = const_cast<JS::SourceTextModule&>(record).link(result->vm());

            // If this throws an exception, set result's error to rethrow to that exception.
            if (linking_result.is_throw_completion()) {
                result->set_error_to_rethrow(linking_result.release_error().value().value());
            }
        } else {
            // FIXME: 4. Otherwise, set result's error to rethrow to parse error.
            TODO();
        }

        // 5. Run onComplete given result.
        on_complete(result);
    });
}

}
