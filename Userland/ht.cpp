#include <LibWeb/Parser/HTMLTokenizer.h>
#include <LibCore/File.h>
#include <AK/ByteBuffer.h>
#include <AK/LogStream.h>

int main(int, char**)
{
    // This is a temporary test program to aid with bringing up the new HTML parser. :^)
    auto file_or_error = Core::File::open("/home/anon/www/simple.html", Core::File::ReadOnly);
    if (file_or_error.is_error())
        return 1;
    auto contents = file_or_error.value()->read_all();
    Web::HTMLTokenizer tokenizer(contents);
    tokenizer.run();
    return 0;
}
