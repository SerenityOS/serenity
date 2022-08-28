/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibWeb/DOM/Text.h>

namespace Web::DOM {

class CDATASection final : public Text {
    WEB_PLATFORM_OBJECT(Text, CDATASection);

public:
    virtual ~CDATASection() override;

    // ^Node
    virtual FlyString node_name() const override { return "#cdata-section"; }

private:
    CDATASection(Document&, String const&);
};

template<>
inline bool Node::fast_is<CDATASection>() const { return is_cdata_section(); }

}

WRAPPER_HACK(CDATASection, Web::DOM)
