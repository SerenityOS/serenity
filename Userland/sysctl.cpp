#include <AK/AKString.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CDirIterator.h>
#include <LibCore/CFile.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

static String read_var(const String& name)
{
    StringBuilder builder;
    builder.append("/proc/sys/");
    builder.append(name);
    auto path = builder.to_string();
    CFile f(path);
    if (!f.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "open: %s", f.error_string());
        exit(1);
    }
    const auto& b = f.read_all();
    if (f.error() < 0) {
        fprintf(stderr, "read: %s", f.error_string());
        exit(1);
    }
    return String((const char*)b.pointer(), b.size(), Chomp);
}

static void write_var(const String& name, const String& value)
{
    StringBuilder builder;
    builder.append("/proc/sys/");
    builder.append(name);
    auto path = builder.to_string();
    CFile f(path);
    if (!f.open(CIODevice::WriteOnly)) {
        fprintf(stderr, "open: %s", f.error_string());
        exit(1);
    }
    f.write(value);
    if (f.error() < 0) {
        fprintf(stderr, "write: %s", f.error_string());
        exit(1);
    }
}

static int handle_show_all()
{
    CDirIterator di("/proc/sys", CDirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "CDirIterator: %s\n", di.error_string());
        return 1;
    }

    while (di.has_next()) {
        String variable_name = di.next_path();
        printf("%s = %s\n", variable_name.characters(), read_var(variable_name).characters());
    }
    return 0;
}

static int handle_var(const String& var)
{
    String spec(var.characters(), Chomp);
    auto parts = spec.split('=');
    String variable_name = parts[0];
    bool is_write = parts.size() > 1;

    if (!is_write) {
        printf("%s = %s\n", variable_name.characters(), read_var(variable_name).characters());
        return 0;
    }

    printf("%s = %s", variable_name.characters(), read_var(variable_name).characters());
    write_var(variable_name, parts[1]);
    printf(" -> %s\n", read_var(variable_name).characters());
    return 0;
}

int main(int argc, char** argv)
{
    CArgsParser args_parser("sysctl");

    args_parser.add_arg("a", "show all variables");
    args_parser.add_single_value("variable=[value]");

    CArgsParserResult args = args_parser.parse(argc, argv);

    if (args.is_present("a")) {
        return handle_show_all();
    } else if (args.get_single_values().size() != 1) {
        args_parser.print_usage();
        return 0;
    }

    Vector<String> values = args.get_single_values();
    return handle_var(values[0]);
}
