/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibWeb/DOM/ChildNode.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NonDocumentTypeChildNode.h>

namespace Web::DOM {

class CharacterData
    : public Node
    , public ChildNode<CharacterData>
    , public NonDocumentTypeChildNode<CharacterData> {
public:
    using WrapperType = Bindings::CharacterDataWrapper;

    virtual ~CharacterData() override;

    const String& data() const { return m_data; }
    void set_data(String);

    unsigned length() const { return m_data.length(); }

protected:
    explicit CharacterData(Document&, NodeType, const String&);

private:
    String m_data;
};

}
