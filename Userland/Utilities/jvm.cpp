#include <LibCore/ArgsParser.h>
#include <AK/Error.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibJVM/JVM.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments) {
    char const* filename = nullptr;
    Core::ArgsParser parser;
    parser.add_positional_argument(filename, "File name to parse", "file");
    parser.parse(arguments);
    if (!filename) {
        warnln("Error: no file provided!");
        return 1;
    }
    StringView file = StringView(filename);
    if (!file.ends_with(StringView(".class"))) {
        warnln("Error: file provided was not a .class file!");
        return 1;
    }
    JVM::JVM jvm = JVM::JVM();
    bool result = jvm.load_from_class_file(file);
    if (!result)
        return 1;


}
