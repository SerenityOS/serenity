/*
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Heap/Cell.h>
#include <LibWeb/HTML/Scripting/ModuleScript.h>

namespace Web::HTML {

class ModuleLocationTuple {
public:
    ModuleLocationTuple(AK::URL url, DeprecatedString type)
        : m_url(move(url))
        , m_type(move(type))
    {
    }

    AK::URL const& url() const { return m_url; };
    DeprecatedString const& type() const { return m_type; }

    bool operator==(ModuleLocationTuple const& other) const
    {
        return other.url() == m_url && other.type() == m_type;
    };

private:
    AK::URL m_url;
    DeprecatedString m_type;
};

// https://html.spec.whatwg.org/multipage/webappapis.html#module-map
class ModuleMap final : public JS::Cell {
    JS_CELL(ModuleMap, Cell);

public:
    ModuleMap() = default;
    ~ModuleMap() = default;

    enum class EntryType {
        Fetching,
        Failed,
        ModuleScript
    };

    struct Entry {
        EntryType type;
        JS::GCPtr<JavaScriptModuleScript> module_script;
    };

    bool is_fetching(AK::URL const& url, DeprecatedString const& type) const;
    bool is_failed(AK::URL const& url, DeprecatedString const& type) const;

    bool is(AK::URL const& url, DeprecatedString const& type, EntryType) const;

    Optional<Entry> get(AK::URL const& url, DeprecatedString const& type) const;

    AK::HashSetResult set(AK::URL const& url, DeprecatedString const& type, Entry);

    void wait_for_change(AK::URL const& url, DeprecatedString const& type, Function<void(Entry)> callback);

private:
    virtual void visit_edges(JS::Cell::Visitor&) override;

    HashMap<ModuleLocationTuple, Entry> m_values;
    HashMap<ModuleLocationTuple, Vector<Function<void(Entry)>>> m_callbacks;
};

}

namespace AK {

template<>
struct Traits<Web::HTML::ModuleLocationTuple> : public GenericTraits<Web::HTML::ModuleLocationTuple> {
    static unsigned hash(Web::HTML::ModuleLocationTuple const& tuple)
    {
        return pair_int_hash(tuple.url().to_deprecated_string().hash(), tuple.type().hash());
    }
};

}
