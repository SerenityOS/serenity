#include <stdio.h>
#include <unistd.h>
#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <getopt.h>
#include <fcntl.h>
#include <AK/AKString.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>

static bool flag_show_all = false;
static int show_all();
static int handle_var(const char*);

static void usage()
{
    printf("usage: sysctl [-a] [variable[=value]]\n");
}

int main(int argc, char** argv)
{
    int opt;
    while ((opt = getopt(argc, argv, "a")) != -1) {
        switch (opt) {
        case 'a':
            flag_show_all = true;
            break;
        default:
            usage();
            return 0;
        }
    }

    if (flag_show_all)
        return show_all();

    if (optind >= argc) {
        usage();
        return 0;
    }

    const char* var = argv[optind];

    return handle_var(var);
}

int show_all()
{
    DIR* dirp = opendir("/proc/sys");
    if (!dirp) {
        perror("opendir");
        return 1;
    }
    char pathbuf[PATH_MAX];

    while (auto* de = readdir(dirp)) {
        if (de->d_name[0] == '.')
            continue;
        sprintf(pathbuf, "/proc/sys/%s", de->d_name);
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
        printf("%s = %s", de->d_name, buffer);
        if (nread && buffer[nread - 1] != '\n')
            printf("\n");
    }
    closedir(dirp);
    return 0;
}

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

int handle_var(const char* var)
{
    String spec(var, Chomp);
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
