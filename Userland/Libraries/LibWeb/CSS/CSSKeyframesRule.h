/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/NonnullRefPtr.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/CSS/CSSKeyframeRule.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::CSS {

// https://drafts.csswg.org/css-animations/#interface-csskeyframesrule
class CSSKeyframesRule final : public CSSRule {
    WEB_PLATFORM_OBJECT(CSSKeyframesRule, CSSRule);

public:
    [[nodiscard]] static JS::NonnullGCPtr<CSSKeyframesRule> create(JS::Realm&, FlyString name, Vector<JS::NonnullGCPtr<CSSKeyframeRule>>);

    virtual ~CSSKeyframesRule() = default;

    virtual Type type() const override { return Type::Keyframes; }

    Vector<JS::NonnullGCPtr<CSSKeyframeRule>> const& keyframes() const { return m_keyframes; }
    FlyString const& name() const { return m_name; }
    size_t length() { return m_keyframes.size(); }

    void set_name(String const& name) { m_name = name; }

private:
    CSSKeyframesRule(JS::Realm& realm, FlyString name, Vector<JS::NonnullGCPtr<CSSKeyframeRule>> keyframes)
        : CSSRule(realm)
        , m_name(move(name))
        , m_keyframes(move(keyframes))
    {
    }

    virtual void visit_edges(Visitor&) override;

    virtual void initialize(JS::Realm&) override;
    virtual DeprecatedString serialized() const override;

    FlyString m_name;
    Vector<JS::NonnullGCPtr<CSSKeyframeRule>> m_keyframes;
};

template<>
inline bool CSSRule::fast_is<CSSKeyframesRule>() const { return type() == CSSRule::Type::Keyframes; }

}
