#include <AK/AKString.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibCore/CFile.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    UNUSED_PARAM(argc);
    UNUSED_PARAM(argv);

    CFile file("/proc/pci");
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", file.error_string());
        return 1;
    }

    auto file_contents = file.read_all();
    auto json = JsonValue::from_string(file_contents).as_array();
    json.for_each([](auto& value) {
        auto dev = value.as_object();

        auto bus = dev.get("bus").to_u32();
        auto slot = dev.get("slot").to_u32();
        auto function = dev.get("function").to_u32();
        auto vendor_id = dev.get("vendor_id").to_u32();
        auto device_id = dev.get("device_id").to_u32();
        auto revision_id = dev.get("revision_id").to_u32();
        auto class_id = dev.get("class").to_u32();

        printf("%02x:%02x.%d %04x: %04x:%04x (rev %02x)\n",
            bus, slot, function,
            class_id, vendor_id, device_id, revision_id
        );
    });

    return 0;
}
