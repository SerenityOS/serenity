/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/String.h>
#include <LibWeb/DOM/CharacterData.h>

namespace Web::DOM {

class Text final : public CharacterData {
public:
    using WrapperType = Bindings::TextWrapper;

    explicit Text(Document&, const String&);
    virtual ~Text() override;

    static NonnullRefPtr<Text> create_with_global_object(Bindings::WindowObject& window, String const& data);

    // ^Node
    virtual FlyString node_name() const override { return "#text"; }
    virtual bool is_editable() const override { return m_always_editable || CharacterData::is_editable(); }

    void set_always_editable(bool b) { m_always_editable = b; }

private:
    virtual RefPtr<Layout::Node> create_layout_node() override;

    bool m_always_editable { false };
};

}
