#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>

#include <stdio.h>
#include <sys/stat.h>

struct WhatToPrint {
    bool line_flag=false;
    bool byte_flag=false;
    bool char_flag=false;
    bool word_flag=false;
};

struct Word {
    bool line=false;
    bool tab=false;
    bool space=false;
};

struct Count {
    unsigned int lines=0;
    unsigned int characters=0;
    unsigned int words=0;
    size_t bytes=0;
};

Vector<String> get_files(int argc, char** argv)
{
    Vector<String> files;
    for (int i=1; i<argc; i++)
    	if (argv[i][0]!='-')
            files.append(argv[i]);
    return files;
}

void wc_out(WhatToPrint& flags, Count& count, String name)
{
    if (flags.line_flag)
        printf("%7i ", count.lines);
    if (flags.word_flag)
        printf("%7i ", count.words);
    if (flags.byte_flag)
        printf("%7lu ", count.bytes);
    if (flags.char_flag)
        printf("%7i ", count.characters);

    printf("%14s\n", name.characters());
}

int main(int argc, char **argv)
{
    struct stat st;
    int current_character;
    int iteration = 0;
    FILE *file_pointer = stdin;
    WhatToPrint arg_flags;
    Word word_flags;
    Count single_count;
    Count total_count;

    CArgsParser args_parser("wc");
    args_parser.add_arg("l", "Output line count");
    args_parser.add_arg("c", "Output byte count");
    args_parser.add_arg("m", "Output character count");
    args_parser.add_arg("w", "Output word count");
    CArgsParserResult args = args_parser.parse(argc, argv);
    if (args.is_present("l")) {
        arg_flags.line_flag=true;
    }
    if (args.is_present("c")) {
        arg_flags.byte_flag=true;
    }
    if (args.is_present("m")) {
        arg_flags.char_flag=true;
    }
    if (args.is_present("w")) {
        arg_flags.word_flag=true;
    }
    if (!arg_flags.line_flag && !arg_flags.char_flag && !arg_flags.word_flag && !arg_flags.byte_flag) {
        arg_flags.line_flag = arg_flags.char_flag = arg_flags.word_flag = true;
    }
    Vector<String> files = get_files(argc, argv);
    unsigned int existing_file_count=files.size();
    do {
        if (files.size() > 0 && (file_pointer = fopen(files[iteration].characters(), "r"))==NULL) {
            fprintf(stderr, "wc: unable to open %s\n", files[iteration].characters());
	    file_count--;
            continue;
        }
        word_flags.line=true;
        while ((current_character = fgetc(file_pointer)) != EOF) {
            single_count.characters++;
            if (current_character>='A' && current_character<='z' && (word_flags.space||word_flags.line||word_flags.tab)) {
                single_count.words++;
                word_flags.space=false;
                word_flags.line=false;
                word_flags.tab=false;
            }
            switch(current_character) {
            case '\n': single_count.lines++;
                       word_flags.line=true;
                       break;
            case ' ': word_flags.space=true;
                      break;
            case '\t': word_flags.tab=true;
                       break;
            }
        }
        fclose(file_pointer);
	    if (file_pointer!=stdin) {
            stat(files[iteration].characters(), &st);
            single_count.bytes=st.st_size;
	    }
        if (files.size() > 1) {
            total_count.lines += single_count.lines;
            total_count.words += single_count.words;
            total_count.characters += single_count.characters;
            total_count.bytes += single_count.bytes;
        }
        if (files.size()>0)
            wc_out(arg_flags, single_count, files[iteration].characters());
        else
            wc_out(arg_flags, single_count, "");
        single_count.lines=0;
        single_count.words=0;
        single_count.characters=0;
        single_count.bytes=0;
    } while (++iteration<files.size());
    if (existing_file_count > 1)
        wc_out(arg_flags, total_count, "total");
}
