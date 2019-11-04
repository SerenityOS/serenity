#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>

#include <stdio.h>
#include <sys/stat.h>

struct Count {
    String name;
    bool exists = true;
    unsigned int lines = 0;
    unsigned int characters = 0;
    unsigned int words = 0;
    size_t bytes = 0;
};

bool output_line = false;
bool output_byte = false;
bool output_character = false;
bool output_word = false;

void wc_out(Count& count)
{
    if (output_line)
        printf("%7i ", count.lines);
    if (output_word)
        printf("%7i ", count.words);
    if (output_byte)
        printf("%7lu ", count.bytes);
    if (output_character)
        printf("%7i ", count.characters);

    printf("%14s\n", count.name.characters());
}

Count get_count(const String& file_name)
{
    Count count;
    FILE* file_pointer = nullptr;
    if (file_name == "-") {
        count.name = "";
        file_pointer = stdin;
    } else {
        count.name = file_name;
        if ((file_pointer = fopen(file_name.characters(), "r")) == NULL) {
            fprintf(stderr, "wc: unable to open %s\n", file_name.characters());
            count.exists = false;
            return count;
        }
    }
    bool tab_flag = false;
    bool space_flag = false;
    bool line_flag = true;
    int current_character;
    while ((current_character = fgetc(file_pointer)) != EOF) {
        count.characters++;
        if (current_character >= 'A' && current_character <= 'z' && (space_flag || line_flag || tab_flag)) {
            count.words++;
            space_flag = false;
            line_flag = false;
            tab_flag = false;
        }
        switch (current_character) {
        case '\n':
            count.lines++;
            line_flag = true;
            break;
        case ' ':
            space_flag = true;
            break;
        case '\t':
            tab_flag = true;
            break;
        }
    }
    fclose(file_pointer);
    if (file_pointer != stdin) {
        struct stat st;
        stat(file_name.characters(), &st);
        count.bytes = st.st_size;
    }
    return count;
}

Count get_total_count(Vector<Count>& counts)
{
    Count total_count{ "total" };
    for (auto& count : counts) {
        total_count.lines += count.lines;
        total_count.words += count.words;
        total_count.characters += count.characters;
        total_count.bytes += count.bytes;
    }
    return total_count;
}

int main(int argc, char** argv)
{
    CArgsParser args_parser("wc");
    args_parser.add_arg("l", "Output line count");
    args_parser.add_arg("c", "Output byte count");
    args_parser.add_arg("m", "Output character count");
    args_parser.add_arg("w", "Output word count");
    args_parser.add_arg("h", "Print help message");
    CArgsParserResult args = args_parser.parse(argc, argv);
    if (args.is_present("h")) {
        args_parser.print_usage();
        return 1;
    }
    if (args.is_present("l")) {
        output_line = true;
    }
    if (args.is_present("w")) {
        output_word = true;
    }
    if (args.is_present("m")) {
        output_character = true;
    }
    if (args.is_present("c")) {
        if (!output_word && !output_line && !output_character)
            output_word = output_line = true;
        output_byte = true;
    }
    if (!output_line && !output_character && !output_word && !output_byte)
        output_line = output_character = output_word = true;

    Vector<String> file_names = args.get_single_values();
    Vector<Count> counts;
    for (auto& file_name : file_names) {
        Count count = get_count(file_name);
        counts.append(count);
    }

    if (file_names.size() > 1) {
        Count total_count = get_total_count(counts);
        counts.append(total_count);
    }

    if (file_names.is_empty()) {
        Count count = get_count("-");
        counts.append(count);
    }

    for (auto& count : counts)
        if (count.exists)
            wc_out(count);

    return 0;
}
