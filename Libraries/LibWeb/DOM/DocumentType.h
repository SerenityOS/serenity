/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/DOM/Node.h>

namespace Web::DOM {

class DocumentType final : public Node {
public:
    using WrapperType = Bindings::DocumentTypeWrapper;

    explicit DocumentType(Document&);
    virtual ~DocumentType() override;

    virtual FlyString node_name() const override { return "#doctype"; }

    const String& name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

    const String& public_id() const { return m_public_id; }
    void set_public_id(const String& public_id) { m_public_id = public_id; }

    const String& system_id() const { return m_system_id; }
    void set_system_id(const String& system_id) { m_system_id = system_id; }

private:
    String m_name;
    String m_public_id;
    String m_system_id;
};

}

AK_BEGIN_TYPE_TRAITS(Web::DOM::DocumentType)
static bool is_type(const Web::DOM::Node& node) { return node.type() == Web::DOM::NodeType::DOCUMENT_TYPE_NODE; }
AK_END_TYPE_TRAITS()
