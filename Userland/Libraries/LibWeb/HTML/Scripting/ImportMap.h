/*
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>

namespace Web::HTML {

using ModuleSpecifierMap = HashMap<DeprecatedString, Optional<AK::URL>>;

// https://html.spec.whatwg.org/multipage/webappapis.html#import-map
class ImportMap {
public:
    ImportMap() = default;

    ModuleSpecifierMap const& imports() const { return m_imports; }
    ModuleSpecifierMap& imports() { return m_imports; }

    HashMap<AK::URL, ModuleSpecifierMap> const& scopes() const { return m_scopes; }
    HashMap<AK::URL, ModuleSpecifierMap>& scopes() { return m_scopes; }

private:
    ModuleSpecifierMap m_imports;
    HashMap<AK::URL, ModuleSpecifierMap> m_scopes;
};

}
