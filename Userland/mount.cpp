#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CFile.h>
#include <stdio.h>
#include <unistd.h>

bool mount_all()
{
    // Mount all filesystems listed in /etc/fstab.
    dbg() << "Mounting all filesystems...";

    auto fstab = CFile::construct("/etc/fstab");
    if (!fstab->open(CIODevice::OpenMode::ReadOnly)) {
        fprintf(stderr, "Failed to open /etc/fstab: %s\n", fstab->error_string());
        return false;
    }

    bool all_ok = true;
    while (fstab->can_read_line()) {
        ByteBuffer buffer = fstab->read_line(1024);
        StringView line_view = (const char*)buffer.data();

        // Trim the trailing newline, if any.
        if (line_view.length() > 0 && line_view[line_view.length() - 1] == '\n')
            line_view = line_view.substring_view(0, line_view.length() - 1);
        String line = line_view;

        Vector<String> parts = line.split('\t');
        if (parts.size() < 3) {
            fprintf(stderr, "Invalid fstab entry: %s\n", line.characters());
            all_ok = false;
            continue;
        }

        const char* devname = parts[0].characters();
        const char* mountpoint = parts[1].characters();
        const char* fstype = parts[2].characters();

        if (strcmp(mountpoint, "/") == 0) {
            dbg() << "Skipping mounting root";
            continue;
        }

        dbg() << "Mounting " << devname << "(" << fstype << ")"
              << " on " << mountpoint;
        int rc = mount(devname, mountpoint, fstype, 0);
        if (rc != 0) {
            fprintf(stderr, "Failed to mount %s (%s) on %s: %s\n", devname, fstype, mountpoint, strerror(errno));
            all_ok = false;
            continue;
        }
    }

    return all_ok;
}

bool print_mounts()
{
    // Output info about currently mounted filesystems.
    auto df = CFile::construct("/proc/df");
    if (!df->open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Failed to open /proc/df: %s\n", df->error_string());
        return false;
    }

    auto content = df->read_all();
    auto json = JsonValue::from_string(content).as_array();

    json.for_each([](auto& value) {
        auto fs_object = value.as_object();
        auto class_name = fs_object.get("class_name").to_string();
        auto mount_point = fs_object.get("mount_point").to_string();
        auto device = fs_object.get("device").as_string_or(class_name);

        printf("%s on %s type %s\n", device.characters(), mount_point.characters(), class_name.characters());
    });

    return true;
}

int main(int argc, char** argv)
{
    CArgsParser args_parser("mount");
    args_parser.add_arg("devname", "device path");
    args_parser.add_arg("mountpoint", "mount point");
    args_parser.add_arg("t", "fstype", "file system type");
    args_parser.add_arg("a", "mount all systems listed in /etc/fstab");
    CArgsParserResult args = args_parser.parse(argc, argv);

    if (args.is_present("a")) {
        return mount_all() ? 0 : 1;
    }

    switch (args.get_single_values().size()) {
    case 0:
        return print_mounts() ? 0 : 1;
    case 2: {
        String devname = args.get_single_values()[0];
        String mountpoint = args.get_single_values()[1];
        String fstype = args.is_present("t") ? args.get("t") : "ext2";

        if (mount(devname.characters(), mountpoint.characters(), fstype.characters(), 0) < 0) {
            perror("mount");
            return 1;
        }
        return 0;
    }
    default:
        args_parser.print_usage();
        return 1;
    }
}
