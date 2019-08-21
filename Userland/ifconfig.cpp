#include <AK/AKString.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/Types.h>
#include <LibCore/CFile.h>
#include <stdio.h>

String si_bytes(unsigned bytes)
{
    if (bytes >= GB)
        return String::format("%fGiB", (double)bytes / (double)GB);
    if (bytes >= MB)
        return String::format("%fMiB", (double)bytes / (double)MB);
    if (bytes >= KB)
        return String::format("%fKiB", (double)bytes / (double)KB);
    return String::format("%dB", bytes);
}

int main(int argc, char** argv)
{
    UNUSED_PARAM(argc);
    UNUSED_PARAM(argv);

    CFile file("/proc/net/adapters");
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", file.error_string());
        return 1;
    }

    auto file_contents = file.read_all();
    auto json = JsonValue::from_string(file_contents).as_array();
    json.for_each([](auto& value) {
        auto if_object = value.as_object();

        auto name = if_object.get("name").to_string();
        auto class_name = if_object.get("class_name").to_string();
        auto mac_address = if_object.get("mac_address").to_string();
        auto ipv4_address = if_object.get("ipv4_address").to_string();
        auto packets_in = if_object.get("packets_in").to_u32();
        auto bytes_in = if_object.get("bytes_in").to_u32();
        auto packets_out = if_object.get("packets_out").to_u32();
        auto bytes_out = if_object.get("bytes_out").to_u32();

        printf("%s:\n", name.characters());
        printf("     mac: %s\n", mac_address.characters());
        printf("    ipv4: %s\n", ipv4_address.characters());
        printf("   class: %s\n", class_name.characters());
        printf("      RX: %u packets %u bytes (%s)\n", packets_in, bytes_in, si_bytes(bytes_in).characters());
        printf("      TX: %u packets %u bytes (%s)\n", packets_out, bytes_out, si_bytes(bytes_out).characters());
        printf("\n");
    });

    return 0;
}
