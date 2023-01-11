/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <LibWeb/DOM/ChildNode.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NonDocumentTypeChildNode.h>

namespace Web::DOM {

class CharacterData
    : public Node
    , public ChildNode<CharacterData>
    , public NonDocumentTypeChildNode<CharacterData> {
    WEB_PLATFORM_OBJECT(CharacterData, Node);

public:
    virtual ~CharacterData() override = default;

    DeprecatedString const& data() const { return m_data; }
    void set_data(DeprecatedString);

    unsigned length() const { return m_data.length(); }

    WebIDL::ExceptionOr<DeprecatedString> substring_data(size_t offset, size_t count) const;
    WebIDL::ExceptionOr<void> append_data(DeprecatedString const&);
    WebIDL::ExceptionOr<void> insert_data(size_t offset, DeprecatedString const&);
    WebIDL::ExceptionOr<void> delete_data(size_t offset, size_t count);
    WebIDL::ExceptionOr<void> replace_data(size_t offset, size_t count, DeprecatedString const&);

protected:
    CharacterData(Document&, NodeType, DeprecatedString const&);

    virtual void initialize(JS::Realm&) override;

private:
    DeprecatedString m_data;
};

}
