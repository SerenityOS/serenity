#include <AK/JsonArray.h>
#include <AK/StringBuilder.h>

namespace AK {

void JsonArray::to_string(StringBuilder& builder) const
{
    builder.append('[');
    for (int i = 0; i < m_values.size(); ++i) {
        m_values[i].to_string(builder);
        if (i != size() - 1)
            builder.append(',');
    }
    builder.append(']');
}

String JsonArray::to_string() const
{
    StringBuilder builder;
    to_string(builder);
    return builder.to_string();
}

}
