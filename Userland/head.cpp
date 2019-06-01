#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <LibCore/CArgsParser.h>

int head(const String& filename, bool print_filename, int line_count);

int main(int argc, char** argv)
{
    CArgsParser args_parser("head");

    args_parser.add_arg("n", "lines", "Number of lines to print (default 10)");
    args_parser.add_arg("q", "Never print filenames");
    args_parser.add_arg("v", "Always print filenames");

    CArgsParserResult args = args_parser.parse(argc, (const char**)argv);

    int line_count = 10;
    if (args.is_present("n")) {
        line_count = strtol(args.get("n").characters(), NULL, 10);
        if (errno) {
            args_parser.print_usage();
            return -1;
        }
    }

    Vector<String> files = args.get_single_values();

    bool print_filenames = files.size() > 1;

    if (args.is_present("v")) {
        print_filenames = true;
    } else if (args.is_present("q")) {
        print_filenames = false;
    }

    if (files.is_empty()) {
        return head("", print_filenames, line_count);
    }

    int rc = 0;

    for (auto &file : files) {
        if (head(file, print_filenames, line_count) != 0) {
            rc = 1;
        }
    }

    return rc;
}

int head(const String& filename, bool print_filename, int line_count)
{
    bool is_stdin = false;
    FILE* fp = nullptr;

    if (filename == "" || filename == "-") {
        fp = stdin;
        is_stdin = true;
    } else {
        fp = fopen(filename.characters(), "r");
        if (!fp) {
            fprintf(stderr, "can't open %s for reading: %s\n", filename.characters(), strerror(errno));
            return 1;
        }
    }

    if (print_filename) {
        if (is_stdin) {
            puts("==> standard input <==");
        } else {
            printf("==> %s <==\n", filename.characters());
        }
    }

    for (int line = 0; line < line_count; ++line) {
        char buffer[BUFSIZ];
        auto* str = fgets(buffer, sizeof(buffer), fp);
        if (!str)
            break;

        // specifically use fputs rather than puts, because fputs doesn't add
        // its own newline.
        fputs(str, stdout);
    }

    fclose(fp);

    if (print_filename) {
        puts("");
    }

    return 0;
}
