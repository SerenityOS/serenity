#include <stdio.h>
#include <unistd.h>
#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <fcntl.h>
#include <AK/AKString.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CDirIterator.h>

static String read_var(const String& name)
{
    StringBuilder builder;
    builder.append("/proc/sys/");
    builder.append(name);
    auto path = builder.to_string();
    int fd = open(path.characters(), O_RDONLY);
    if (fd < 0) {
        perror("open");
        exit(1);
    }
    char buffer[1024];
    int nread = read(fd, buffer, sizeof(buffer));
    close(fd);
    if (nread < 0) {
        perror("read");
        exit(1);
    }
    return String(buffer, nread, Chomp);
}

static void write_var(const String& name, const String& value)
{
    StringBuilder builder;
    builder.append("/proc/sys/");
    builder.append(name);
    auto path = builder.to_string();
    int fd = open(path.characters(), O_WRONLY);
    if (fd < 0) {
        perror("open");
        exit(1);
    }
    int nwritten = write(fd, value.characters(), value.length());
    if (nwritten < 0) {
        perror("read");
        exit(1);
    }
    close(fd);
}


static int handle_show_all()
{
    CDirIterator di("/proc/sys", CDirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "CDirIterator: %s\n", di.error_string());
        return 1;
    }

    char pathbuf[PATH_MAX];
    while (di.has_next()) {
        String name = di.next_path();
        sprintf(pathbuf, "/proc/sys/%s", name.characters());
        int fd = open(pathbuf, O_RDONLY);
        if (fd < 0) {
            perror("open");
            continue;
        }
        char buffer[1024];
        int nread = read(fd, buffer, sizeof(buffer));
        close(fd);
        if (nread < 0) {
            perror("read");
            continue;
        }
        buffer[nread] = '\0';
        printf("%s = %s", name.characters(), buffer);
        if (nread && buffer[nread - 1] != '\n')
            printf("\n");
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

    CArgsParserResult args = args_parser.parse(argc, (const char**)argv);

    if (args.is_present("a")) {
        return handle_show_all();
    } else if (args.get_single_values().size() != 1) {
        args_parser.print_usage();
        return 0;
    }

    Vector<String> values = args.get_single_values();
    return handle_var(values[0]);
}

