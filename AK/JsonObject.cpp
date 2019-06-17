#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>

String JsonObject::to_string() const
{
    StringBuilder builder;
    int index = 0;
    builder.append('{');
    for_each_member([&] (auto& key, auto& value) {
        builder.append('"');
        builder.append(key);
        builder.append('"');
        builder.append(':');
        builder.append(value.to_string());
        if (index != size() - 1)
            builder.append(',');
        ++index;
    });
    builder.append('}');
    return builder.to_string();
}
