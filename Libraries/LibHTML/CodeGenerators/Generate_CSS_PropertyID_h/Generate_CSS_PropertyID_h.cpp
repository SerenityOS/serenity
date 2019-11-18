#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>
#include <ctype.h>
#include <stdio.h>

static String title_casify(const String& dashy_name)
{
    auto parts = dashy_name.split('-');
    StringBuilder builder;
    for (auto& part : parts) {
        if (part.is_empty())
            continue;
        builder.append(toupper(part[0]));
        if (part.length() == 1)
            continue;
        builder.append(part.substring_view(1, part.length() - 1));
    }
    return builder.to_string();
}

int main(int argc, char** argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: %s <path/to/CSS/Properties.json>\n", argv[0]);
        return 1;
    }
    auto file = CFile::construct(argv[1]);
    if (!file->open(CIODevice::ReadOnly))
        return 1;

    auto json = JsonValue::from_string(file->read_all());
    ASSERT(json.is_object());

    dbg() << "#pragma once";
    dbg() << "#include <AK/StringView.h>";
    dbg() << "#include <AK/Traits.h>";

    dbg() << "namespace CSS {";
    dbg() << "enum class PropertyID {";
    dbg() << "    Invalid,";

    json.as_object().for_each_member([&](auto& name, auto& value) {
        ASSERT(value.is_object());
        dbg() << "    " << title_casify(name) << ",";
    });

    dbg() << "};\n\
PropertyID property_id_from_string(const StringView&);\n\
const char* string_from_property_id(PropertyID);\n\
}\n\
\n\
namespace AK {\n\
template<>\n\
struct Traits<CSS::PropertyID> : public GenericTraits<CSS::PropertyID> {\n\
    static unsigned hash(CSS::PropertyID property_id) { return int_hash((unsigned)property_id); }\n\
};\n\
}\n";

    return 0;
}
