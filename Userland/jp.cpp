#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>
#include <stdio.h>

static void print(const JsonValue& value, int indent = 0);
static void print_indent(int indent)
{
    for (int i = 0; i < indent; ++i)
        printf("  ");
}

int main(int argc, char** argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: jp <file>\n");
        return 0;
    }
    CFile file(argv[1]);
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Couldn't open %s for reading: %s\n", argv[1], file.error_string());
        return 1;
    }

    auto file_contents = file.read_all();
    auto json = JsonValue::from_string(file_contents);

    print(json);
    printf("\n");

    return 0;
}

void print(const JsonValue& value, int indent)
{
    if (value.is_object()) {
        printf("{\n");
        value.as_object().for_each_member([&](auto& member_name, auto& member_value) {
            print_indent(indent + 1);
            printf("\"\033[33;1m%s\033[0m\": ", member_name.characters());
            print(member_value, indent + 1);
            printf(",\n");
        });
        print_indent(indent);
        printf("}");
        return;
    }
    if (value.is_array()) {
        printf("[\n");
        value.as_array().for_each([&](auto& entry_value) {
            print_indent(indent + 1);
            print(entry_value, indent + 1);
            printf(",\n");
        });
        print_indent(indent);
        printf("]");
        return;
    }
    if (value.is_string())
        printf("\033[31;1m");
    else if (value.is_number())
        printf("\033[35;1m");
    else if (value.is_bool())
        printf("\033[32;1m");
    else if (value.is_null() || value.is_undefined())
        printf("\033[34;1m");
    printf("%s", value.to_string().characters());
    printf("\033[0m");
}
