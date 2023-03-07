#include <AK/Format.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int hello_count = 1;
    StringView epilog;

    Core::ArgsParser parser;
    parser.add_option(hello_count, "How often to print \"Hello friends!\"", "count", 'c', "hello-count");
    parser.add_positional_argument(epilog, "What to print at the end", "epilog", Core::ArgsParser::Required::No);
    parser.parse(arguments);

    for (auto i = 0; i < hello_count; ++i)
        outln("Hello friends!");

    if (!epilog.is_empty())
        outln("And finally: {}", epilog);

    return 0;
}
