#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>

namespace AK {

void JsonObject::to_string(StringBuilder& builder) const
{
    int index = 0;
    builder.append('{');
    for_each_member([&] (auto& key, auto& value) {
        builder.append('"');
        builder.append(key);
        builder.append('"');
        builder.append(':');
        value.to_string(builder);
        if (index != size() - 1)
            builder.append(',');
        ++index;
    });
    builder.append('}');
}

String JsonObject::to_string() const
{
    StringBuilder builder;
    to_string(builder);
    return builder.to_string();
}

}
