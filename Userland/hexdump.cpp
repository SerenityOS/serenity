#include <AK/LogStream.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <ctype.h>

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    const char* path = nullptr;
    args_parser.add_positional_argument(path, "Input", "input", Core::ArgsParser::Required::No);

    args_parser.parse(argc, argv);

    RefPtr<Core::File> file;

    if (!path) {
        file = Core::File::construct();
        bool success = file->open(STDIN_FILENO, Core::File::ReadOnly, Core::File::ShouldCloseFileDescription::No);
        ASSERT(success);
    } else {
        auto file_or_error = Core::File::open(path, Core::File::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Failed to open {}: {}", path, file_or_error.error());
            return 1;
        }
        file = file_or_error.value();
    }

    auto contents = file->read_all();

    Vector<u8, 16> line;

    auto print_line = [&] {
        for (size_t i = 0; i < 16; ++i) {
            if (i < line.size())
                printf("%02x ", line[i]);
            else
                printf("   ");

            if (i == 7)
                printf("  ");
        }

        printf("  ");

        for (size_t i = 0; i < 16; ++i) {
            if (i < line.size() && isprint(line[i]))
                putchar(line[i]);
            else
                putchar(' ');
        }

        putchar('\n');
    };

    for (size_t i = 0; i < contents.size(); ++i) {
        line.append(contents[i]);

        if (line.size() == 16) {
            print_line();
            line.clear();
        }
    }

    if (!line.is_empty())
        print_line();

    return 0;
}