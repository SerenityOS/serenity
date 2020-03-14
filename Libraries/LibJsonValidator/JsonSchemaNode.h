/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include "Forward.h"
#include <AK/HashMap.h>
#include <AK/JsonValue.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/String.h>

namespace JsonValidator {

enum class InstanceType : u8 {
    //Null,
    //Boolean,
    Object,
    Array,
    //Number,
    String,
};

String to_string(InstanceType type);

class JsonSchemaNode {
public:
    virtual ~JsonSchemaNode() {}
    virtual const char* class_name() const = 0;
    virtual void dump(int indent) const;

    void set_default(JsonValue default_value)
    {
        m_default_value = default_value;
    }

    void set_id(String id)
    {
        m_id = id;
    }

    void set_type(InstanceType type)
    {
        m_type = type;
    }

protected:
    JsonSchemaNode() {}
    JsonSchemaNode(String id, InstanceType type)
        : m_id(move(id))
        , m_type(type)
    {
    }

private:
    String m_id;
    InstanceType m_type;
    JsonValue m_default_value;
};

class StringNode : public JsonSchemaNode {
public:
    StringNode(String id)
        : JsonSchemaNode(id, InstanceType::String)
    {
    }

    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "StringNode"; }
};

class ObjectNode : public JsonSchemaNode {
public:
    ObjectNode()
        : JsonSchemaNode("", InstanceType::Object)
    {
        m_properties = HashMap<String, JsonSchemaNode*>();
    }

    ObjectNode(String id)
        : JsonSchemaNode(id, InstanceType::Object)
    {
        m_properties = HashMap<String, JsonSchemaNode*>();
    }

    void append_property(const String& name, JsonSchemaNode* node)
    {
        m_properties.set(name, node);
    }

    virtual void dump(int indent) const override;

    void append_required(String required)
    {
        m_required.append(required);
    }

    const HashMap<String, JsonSchemaNode*>& properties() const { return m_properties; }
    const Vector<String>& required() const { return m_required; }

private:
    virtual const char* class_name() const override { return "ObjectNode"; }

    HashMap<String, JsonSchemaNode*> m_properties;
    Vector<String> m_required;
};

class ArrayNode : public JsonSchemaNode {
public:
    ArrayNode()
        : JsonSchemaNode("", InstanceType::Array)
    {
    }
    ArrayNode(String id)
        : JsonSchemaNode(id, InstanceType::Array)
    {
    }

    ~ArrayNode()
    {
        if (m_items)
            delete m_items;
    }

    virtual void dump(int indent) const override;

    JsonSchemaNode& items() { return *m_items; }
    void set_items(JsonSchemaNode*&& items) { m_items = items; }

    bool unique_items() { return m_unique_items; }
    void set_unique_items(bool enabled) { m_unique_items = enabled; }

private:
    virtual const char* class_name() const override { return "ArrayNode"; }

    JsonSchemaNode* m_items;
    bool m_unique_items { false };
};

}
