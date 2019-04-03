#include <LibGUI/GApplication.h>
#include "TaskbarWindow.h"

int main(int argc, char** argv)
{
    GApplication app(argc, argv);
    TaskbarWindow window;
    window.show();
    return app.exec();
}
