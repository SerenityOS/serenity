#include <AK/Format.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    outln("Hello friends!");
    return 0;
}
