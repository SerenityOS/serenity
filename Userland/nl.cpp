#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>

#include <stdio.h>
#include <string.h>

int main(int argc, char** argv)
{
    int previous_char;
    int next_char;
    bool all_lines_flag = false;
    bool delimiter_flag = false;
    bool line_numbers_flag = true;
    char delimiter[64];
    FILE* file_pointer = stdin;
    long line_number = 1;
    long line_number_increment = 1;
    unsigned int line_number_width = 6;
    String value_of_b;
    String value_of_i;
    String value_of_s;
    String value_of_v;
    String value_of_w;
    CArgsParser args_parser("nl");
    args_parser.add_arg("b", "type", "Line count type. \n\tt counts non-empty lines. \n\ta counts all lines. \n\tn counts no lines.");
    args_parser.add_arg("i", "incr", "Set line count increment.");
    args_parser.add_arg("s", "delim", "Set buffer between the line numbers and text. 1-63 bytes");
    args_parser.add_arg("v", "startnum", "Initial value used to number logical page lines.");
    args_parser.add_arg("w", "width", "The number of characters used for the line number.");
    args_parser.add_single_value("file");
    CArgsParserResult args = args_parser.parse(argc, argv);
    if (args.is_present("b")) {
        value_of_b = args.get("b");
        if (value_of_b == "a")
            all_lines_flag = true;
        else if (value_of_b == "t")
            all_lines_flag = false;
        else if (value_of_b == "n")
            line_numbers_flag = false;
        else {
            args_parser.print_usage();
            return 1;
        }
    }
    if (args.is_present("i")) {
        value_of_i = args.get("i");
        line_number_increment = atol(value_of_i.characters());
        if (!line_number_increment) {
            args_parser.print_usage();
            return 1;
        }
    }
    if (args.is_present("s")) {
        value_of_s = args.get("s");
        if (value_of_s.length() > 0 && value_of_s.length() < 64)
            delimiter_flag = true;
        else {
            args_parser.print_usage();
            return 1;
        }
    }
    strcpy(delimiter, delimiter_flag ? value_of_s.characters() : "  ");
    if (args.is_present("v")) {
        value_of_v = args.get("v");
        line_number = atol(value_of_v.characters());
    }
    if (args.is_present("w")) {
        value_of_w = args.get("w");
        line_number_width = atol(value_of_w.characters());
    }
    Vector<String> files = args.get_single_values();
    line_number -= line_number_increment; // so the line number can start at 1 when added below
    int i = 0;
    do {
        if (files.size() > 0 && (file_pointer = fopen(files[i].characters(), "r")) == NULL) {
            fprintf(stderr, "unable to open %s\n", files[i].characters());
            continue;
        }
        previous_char = 0;
        next_char = 0;
        while ((next_char = fgetc(file_pointer)) != EOF) {
            if (previous_char == 0 || previous_char == '\n') {
                if (!all_lines_flag && next_char == '\n') {
                    // skips printing line count on empty lines if all_lines_flags is false
                    printf("\n");
                    continue;
                }
                if (line_numbers_flag)
                    printf("%*lu%s", line_number_width, (line_number += line_number_increment), delimiter);
                else
                    printf("%*s", line_number_width, "");
            }
            putchar(next_char);
            previous_char = next_char;
        }
        fclose(file_pointer);
        if (previous_char != '\n')
            printf("\n"); // for cases where files have no trailing newline
    } while (++i < files.size());
    return 0;
}
