#pragma once

#include <AK/JsonValue.h>
#include <AK/Vector.h>

namespace AK {

class JsonArray {
public:
    JsonArray() {}
    ~JsonArray() {}

    int size() const { return m_values.size(); }
    bool is_empty() const { return m_values.is_empty(); }

    const JsonValue& at(int index) const { return m_values.at(index); }
    const JsonValue& operator[](int index) const { return at(index); }

    void clear() { m_values.clear(); }
    void append(const JsonValue& value) { m_values.append(value); }

    String serialized() const;
    void serialize(StringBuilder&) const;

    template<typename Callback>
    void for_each(Callback callback) const
    {
        for (auto& value : m_values)
            callback(value);
    }

    const Vector<JsonValue>& values() const { return m_values; }

private:
    Vector<JsonValue> m_values;
};

}

using AK::JsonArray;
