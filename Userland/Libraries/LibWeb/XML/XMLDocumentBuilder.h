/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/Text.h>
#include <LibXML/Parser/Parser.h>

namespace Web {

enum class XMLScriptingSupport {
    Disabled,
    Enabled,
};

ErrorOr<String> resolve_xml_resource(XML::SystemID const&, Optional<XML::PublicID> const&);

class XMLDocumentBuilder final : public XML::Listener {
public:
    XMLDocumentBuilder(DOM::Document& document, XMLScriptingSupport = XMLScriptingSupport::Enabled);

    bool has_error() const { return m_has_error; }

private:
    virtual void element_start(XML::Name const& name, HashMap<XML::Name, String> const& attributes) override;
    virtual void element_end(XML::Name const& name) override;
    virtual void text(String const& data) override;
    virtual void comment(String const& data) override;
    virtual void document_end() override;

    DOM::Document& m_document;
    DOM::Node* m_current_node { nullptr };
    XMLScriptingSupport m_scripting_support { XMLScriptingSupport::Enabled };
    bool m_has_error { false };
    StringBuilder text_builder;
};

}
