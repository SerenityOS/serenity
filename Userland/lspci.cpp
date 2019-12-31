#include <AK/String.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibCore/CFile.h>
#include <LibPCIDB/Database.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    UNUSED_PARAM(argc);
    UNUSED_PARAM(argv);

    auto db = PCIDB::Database::open();
    if (!db)
        fprintf(stderr, "Couldn't open PCI ID database\n");

    auto proc_pci = CFile::construct("/proc/pci");
    if (!proc_pci->open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", proc_pci->error_string());
        return 1;
    }

    auto file_contents = proc_pci->read_all();
    auto json = JsonValue::from_string(file_contents).as_array();
    json.for_each([db](auto& value) {
        auto dev = value.as_object();
        auto seg = dev.get("seg").to_u32();
        auto bus = dev.get("bus").to_u32();
        auto slot = dev.get("slot").to_u32();
        auto function = dev.get("function").to_u32();
        auto vendor_id = dev.get("vendor_id").to_u32();
        auto device_id = dev.get("device_id").to_u32();
        auto revision_id = dev.get("revision_id").to_u32();
        auto class_id = dev.get("class").to_u32();

        String vendor_name = String::format("%02x", vendor_id);
        auto vendor = db->get_vendor(vendor_id);
        if (vendor != "")
            vendor_name = vendor;

        String device_name = String::format("%02x", device_id);
        auto device = db->get_device(vendor_id, device_id);
        if (device != "")
            device_name = device;

        String class_name = String::format("%04x", class_id);
        auto class_ptr = db->get_class(class_id);
        if (class_ptr != "")
            class_name = class_ptr;

        printf("%04x:%02x:%02x.%d %s: %s %s (rev %02x)\n",
            seg, bus, slot, function,
            class_name.characters(), vendor_name.characters(),
            device_name.characters(), revision_id);
    });

    return 0;
}
