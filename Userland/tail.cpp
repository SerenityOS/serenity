#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <AK/Assertions.h>
#include <LibCore/CFile.h>
#include <LibCore/CArgsParser.h>

#define DEFAULT_LINE_COUNT 10

int tail_from_pos(CFile& file, off_t startline, bool want_follow)
{
    if (!file.seek(startline + 1))
        return 1;

    while (true) {
        const auto& b = file.read(4096);
        if (b.is_empty()) {
            if (!want_follow) {
                break;
            } else {
                while (!file.can_read()) {
                    // FIXME: would be nice to have access to can_read_from_fd with an infinite timeout
                    usleep(100);
                }
                continue;
            }
        }

        if (write(STDOUT_FILENO, b.pointer(), b.size()) < 0)
            return 1;
    }

    return 0;
}

off_t find_seek_pos(CFile& file, int wanted_lines)
{
    // Rather than reading the whole file, start at the end and work backwards,
    // stopping when we've found the number of lines we want.
    off_t pos = 0;
    if (!file.seek(0, CIODevice::SeekMode::FromEndPosition, &pos)) {
        fprintf(stderr, "Failed to find end of file: %s\n", file.error_string());
        return 1;
    }

    off_t end = pos;
    int lines = 0;

    // FIXME: Reading char-by-char is only OK if CIODevice's read buffer
    // is smart enough to not read char-by-char. Fix it there, or fix it here :)
    for(; pos >= 0; pos--) {
        file.seek(pos);
        const auto& ch = file.read(1);
        if (ch.is_empty()) {
            // Presumably the file got truncated?
            // Keep trying to read backwards...
        } else {
            if (*ch.pointer() == '\n' && (end - pos) > 1) {
                lines++;
                if (lines == wanted_lines)
                    break;
            }
        }
    }

    return pos;
}

static void exit_because_we_wanted_lines()
{
    fprintf(stderr, "Expected a line count after -n");
    exit(1);
}

int main(int argc, char *argv[])
{
    CArgsParser args_parser("tail");

    args_parser.add_arg("f", "follow -- appended data is output as it is written to the file");
    args_parser.add_arg("n", "lines", "fetch the specified number of lines");
    args_parser.add_required_single_value("file");

    CArgsParserResult args = args_parser.parse(argc, (const char**)argv);

    Vector<String> values = args.get_single_values();
    if (values.size() != 1) {
        args_parser.print_usage();
        return 1;
    }

    int line_count = 0;
    if (args.is_present("n")) {
        line_count = strtol(args.get("n").characters(), NULL, 10);
        if (errno == EINVAL) {
            args_parser.print_usage();
            return 1;
        }
    } else {
    	line_count = DEFAULT_LINE_COUNT;
    }

    CFile f(values[0]);
    if (!f.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error opening file %s: %s\n", f.filename().characters(), strerror(errno));
        exit(1);
    }

    bool flag_follow = args.is_present("f");
    bool o_arg = args.is_present("o");

    auto pos = find_seek_pos(f, line_count);
    return tail_from_pos(f, pos, flag_follow);
}
