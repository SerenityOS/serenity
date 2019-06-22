#include <AK/AKString.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>

#include <ctype.h>
#include <stdio.h>

static bool output_chars = false;
static bool output_words = false;
static bool output_lines = false;

struct Count {
    String file;
    unsigned long chars = 0;
    unsigned long words = 0;
    unsigned long lines = 0;
};

void report(const Count& count)
{
    if (output_lines) {
        printf("%lu ", count.lines);
    }
    if (output_words) {
        printf("%lu ", count.words);
    }
    if (output_chars) {
        printf("%lu ", count.chars);
    }
    printf("%s\n", count.file.characters());
}

void report(const Vector<Count>& counts)
{
    Count total { "total" };
    for (const auto& c : counts) {
        report(c);
        total.lines += c.lines;
        total.words += c.words;
        total.chars += c.chars;
    }
    if (counts.size() > 1) {
        report(total);
    }
    fflush(stdout);
}

int count_words(const char* s)
{
    int n = 0;
    bool in_word = false;
    for (; *s; ++s) {
        if (!isspace(*s)) {
            if (!in_word) {
                in_word = true;
                ++n;
            }
        } else if (in_word) {
            in_word = false;
        }
    }
    return n;
}

int main(int argc, char** argv)
{
    if (argc < 2) {
        printf("usage: wc [-c|-m] [-lw] [file...]");
        return 0;
    }

    CArgsParser args_parser("wc");
    args_parser.add_arg("l", "Include lines in count");
    args_parser.add_arg("c", "Include bytes in count");
    args_parser.add_arg("m", "Include chars in count");
    args_parser.add_arg("w", "Include words in count");
    CArgsParserResult args = args_parser.parse(argc, (char**)argv);

    if (args.is_present("l")) {
        output_lines = true;
    }
    if (args.is_present("c")) {
        output_chars = true;
    }
    if (args.is_present("m")) {
        output_chars = true;
    }
    if (args.is_present("w")) {
        output_words = true;
    }
    if (!output_lines && !output_words && !output_chars) {
        output_lines = output_chars = output_words = true;
    }

    Vector<String> files = args.get_single_values();
    if (files.is_empty()) {
        fprintf(stderr, "wc: No files provided");
        return 1;
    }

    Vector<Count> counts;
    for (const auto& f : files) {
        FILE* fp = fopen(f.characters(), "r");
        if (fp == nullptr) {
            fprintf(stderr, "wc: Could not open file '%s'\n", f.characters());
            return 1;
        }

        Count count { f };
        char* line = nullptr;
        size_t len = 0;
        ssize_t n_read = 0;
        while ((n_read = getline(&line, &len, fp)) != -1) {
            count.lines++;
            count.words += count_words(line);
            count.chars += n_read;
        }

        counts.append(count);
        fclose(fp);
        if (line) {
            free(line);
        }
    }

    report(counts);
    return 0;
}
