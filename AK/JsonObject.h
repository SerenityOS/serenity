#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/JsonValue.h>

namespace AK {

class JsonObject {
public:
    JsonObject() { }
    ~JsonObject() { }

    int size() const { return m_members.size(); }
    bool is_empty() const { return m_members.is_empty(); }

    JsonValue get(const String& key) const
    {
        auto it = m_members.find(key);
        if (it == m_members.end())
            return JsonValue(JsonValue::Type::Undefined);
        return (*it).value;
    }

    void set(const String& key, JsonValue&& value)
    {
        m_members.set(key, move(value));
    }

    void set(const String& key, const JsonValue& value)
    {
        m_members.set(key, JsonValue(value));
    }

    template<typename Callback>
    void for_each_member(Callback callback) const
    {
        for (auto& it : m_members)
            callback(it.key, it.value);
    }

    String serialized() const;
    void serialize(StringBuilder&) const;

private:
    HashMap<String, JsonValue> m_members;
};

}

using AK::JsonObject;
