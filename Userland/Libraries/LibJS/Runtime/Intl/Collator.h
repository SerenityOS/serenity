/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibJS/Runtime/Intl/CollatorCompareFunction.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class Collator final : public Object {
    JS_OBJECT(Collator, Object);
    JS_DECLARE_ALLOCATOR(Collator);

public:
    enum class Usage {
        Sort,
        Search,
    };

    enum class Sensitivity {
        Base,
        Accent,
        Case,
        Variant,
    };

    enum class CaseFirst {
        Upper,
        Lower,
        False,
    };

    static constexpr auto relevant_extension_keys()
    {
        // 10.2.3 Internal slots, https://tc39.es/ecma402/#sec-intl-collator-internal-slots
        // The value of the [[RelevantExtensionKeys]] internal slot is a List that must include the element "co", may include any or all of the elements "kf" and "kn", and must not include any other elements.
        return AK::Array { "co"sv, "kf"sv, "kn"sv };
    }

    virtual ~Collator() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    Usage usage() const { return m_usage; }
    void set_usage(StringView usage);
    StringView usage_string() const;

    Sensitivity sensitivity() const { return m_sensitivity; }
    void set_sensitivity(StringView sensitivity);
    StringView sensitivity_string() const;

    CaseFirst case_first() const { return m_case_first; }
    void set_case_first(StringView case_first);
    StringView case_first_string() const;

    String const& collation() const { return m_collation; }
    void set_collation(String collation) { m_collation = move(collation); }

    bool ignore_punctuation() const { return m_ignore_punctuation; }
    void set_ignore_punctuation(bool ignore_punctuation) { m_ignore_punctuation = ignore_punctuation; }

    bool numeric() const { return m_numeric; }
    void set_numeric(bool numeric) { m_numeric = numeric; }

    CollatorCompareFunction* bound_compare() const { return m_bound_compare; }
    void set_bound_compare(CollatorCompareFunction* bound_compare) { m_bound_compare = bound_compare; }

private:
    explicit Collator(Object& prototype);

    virtual void visit_edges(Visitor&) override;

    String m_locale;                                    // [[Locale]]
    Usage m_usage { Usage::Sort };                      // [[Usage]]
    Sensitivity m_sensitivity { Sensitivity::Variant }; // [[Sensitivity]]
    CaseFirst m_case_first { CaseFirst::False };        // [[CaseFirst]]
    String m_collation;                                 // [[Collation]]
    bool m_ignore_punctuation { false };                // [[IgnorePunctuation]]
    bool m_numeric { false };                           // [[Numeric]]
    GCPtr<CollatorCompareFunction> m_bound_compare;     // [[BoundCompare]]
};

}
