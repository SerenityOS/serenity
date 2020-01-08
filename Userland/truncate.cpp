#include <LibCore/CArgsParser.h>

#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

enum TruncateOperation {
    OP_Set,
    OP_Grow,
    OP_Shrink,
};

int main(int argc, char** argv)
{
    CArgsParser args_parser("truncate");

    args_parser.add_arg("s", "size", "Resize the target file to (or by) this size. Prefix with + or - to expand or shrink the file, or a bare number to set the size exactly.");
    args_parser.add_arg("r", "reference", "Resize the target file to match the size of this one.");
    args_parser.add_required_single_value("file");

    CArgsParserResult args = args_parser.parse(argc, argv);

    if (!args.is_present("s") && !args.is_present("r")) {
        args_parser.print_usage();
        return -1;
    }

    if (args.is_present("s") && args.is_present("r")) {
        args_parser.print_usage();
        return -1;
    }

    auto op = OP_Set;
    int size = 0;

    if (args.is_present("s")) {
        auto str = args.get("s");

        switch (str[0]) {
        case '+':
            op = OP_Grow;
            str = str.substring(1, str.length() - 1);
            break;
        case '-':
            op = OP_Shrink;
            str = str.substring(1, str.length() - 1);
            break;
        }

        bool ok;
        size = str.to_int(ok);
        if (!ok) {
            args_parser.print_usage();
            return -1;
        }
    }

    if (args.is_present("r")) {
        struct stat st;
        int rc = stat(args.get("r").characters(), &st);
        if (rc < 0) {
            perror("stat");
            return -1;
        }

        op = OP_Set;
        size = st.st_size;
    }

    auto name = args.get_single_values()[0];

    int fd = open(name.characters(), O_RDWR | O_CREAT, 0666);
    if (fd < 0) {
        perror("open");
        return -1;
    }

    struct stat st;
    if (fstat(fd, &st) < 0) {
        perror("fstat");
        return -1;
    }

    switch (op) {
    case OP_Set:
        break;
    case OP_Grow:
        size = st.st_size + size;
        break;
    case OP_Shrink:
        size = st.st_size - size;
        break;
    }

    if (ftruncate(fd, size) < 0) {
        perror("ftruncate");
        return -1;
    }

    if (close(fd) < 0) {
        perror("close");
        return -1;
    }

    return 0;
}
