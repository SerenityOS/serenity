#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>

namespace AK {

void JsonObject::serialize(StringBuilder& builder) const
{
    int index = 0;
    builder.append('{');
    for_each_member([&] (auto& key, auto& value) {
        builder.append('"');
        builder.append(key);
        builder.append('"');
        builder.append(':');
        value.serialize(builder);
        if (index != size() - 1)
            builder.append(',');
        ++index;
    });
    builder.append('}');
}

String JsonObject::serialized() const
{
    StringBuilder builder;
    serialize(builder);
    return builder.to_string();
}

}
