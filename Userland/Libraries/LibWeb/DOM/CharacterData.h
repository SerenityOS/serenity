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

// https://dom.spec.whatwg.org/#characterdata
class CharacterData
    : public Node
    , public ChildNode<CharacterData>
    , public NonDocumentTypeChildNode<CharacterData> {
    WEB_PLATFORM_OBJECT(CharacterData, Node);
    JS_DECLARE_ALLOCATOR(CharacterData);

public:
    virtual ~CharacterData() override = default;

    String const& data() const { return m_data; }
    void set_data(String const&);

    unsigned length_in_utf16_code_units() const
    {
        // FIXME: This is inefficient!
        auto utf16_data = MUST(AK::utf8_to_utf16(m_data));
        return Utf16View { utf16_data }.length_in_code_units();
    }

    WebIDL::ExceptionOr<String> substring_data(size_t offset_in_utf16_code_units, size_t count_in_utf16_code_units) const;
    WebIDL::ExceptionOr<void> append_data(String const&);
    WebIDL::ExceptionOr<void> insert_data(size_t offset_in_utf16_code_units, String const&);
    WebIDL::ExceptionOr<void> delete_data(size_t offset_in_utf16_code_units, size_t count_in_utf16_code_units);
    WebIDL::ExceptionOr<void> replace_data(size_t offset_in_utf16_code_units, size_t count_in_utf16_code_units, String const&);

protected:
    CharacterData(Document&, NodeType, String const&);

    virtual void initialize(JS::Realm&) override;

private:
    String m_data;
};

}
