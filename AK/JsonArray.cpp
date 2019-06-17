#include <AK/JsonArray.h>
#include <AK/StringBuilder.h>

String JsonArray::to_string() const
{
    StringBuilder builder;
    builder.append('[');
    for (int i = 0; i < m_values.size(); ++i) {
        builder.append(m_values[i].to_string());
        if (i != size() - 1)
            builder.append(',');
    }
    builder.append(']');
    return builder.to_string();
}
