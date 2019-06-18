#include <AK/JsonArray.h>
#include <AK/StringBuilder.h>

namespace AK {

void JsonArray::serialize(StringBuilder& builder) const
{
    builder.append('[');
    for (int i = 0; i < m_values.size(); ++i) {
        m_values[i].serialize(builder);
        if (i != size() - 1)
            builder.append(',');
    }
    builder.append(']');
}

String JsonArray::serialized() const
{
    StringBuilder builder;
    serialize(builder);
    return builder.to_string();
}

}
