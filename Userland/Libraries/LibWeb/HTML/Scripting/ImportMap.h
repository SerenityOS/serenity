/*
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibURL/URL.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

using ModuleSpecifierMap = HashMap<ByteString, Optional<URL::URL>>;

// https://html.spec.whatwg.org/multipage/webappapis.html#import-map
class ImportMap {
public:
    ImportMap() = default;

    ModuleSpecifierMap const& imports() const { return m_imports; }
    ModuleSpecifierMap& imports() { return m_imports; }
    void set_imports(ModuleSpecifierMap const& imports) { m_imports = imports; }

    HashMap<URL::URL, ModuleSpecifierMap> const& scopes() const { return m_scopes; }
    HashMap<URL::URL, ModuleSpecifierMap>& scopes() { return m_scopes; }
    void set_scopes(HashMap<URL::URL, ModuleSpecifierMap> const& scopes) { m_scopes = scopes; }

private:
    ModuleSpecifierMap m_imports;
    HashMap<URL::URL, ModuleSpecifierMap> m_scopes;
};

WebIDL::ExceptionOr<ImportMap> parse_import_map_string(JS::Realm& realm, ByteString const& input, URL::URL base_url);
WebIDL::ExceptionOr<Optional<DeprecatedFlyString>> normalise_specifier_key(JS::Realm& realm, DeprecatedFlyString specifier_key, URL::URL base_url);
WebIDL::ExceptionOr<ModuleSpecifierMap> sort_and_normalise_module_specifier_map(JS::Realm& realm, JS::Object& original_map, URL::URL base_url);
WebIDL::ExceptionOr<HashMap<URL::URL, ModuleSpecifierMap>> sort_and_normalise_scopes(JS::Realm& realm, JS::Object& original_map, URL::URL base_url);

}
