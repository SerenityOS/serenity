#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>
#include <sys/utsname.h>

static int run_shutdown_dialog(int argc, char** argv);

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: SystemDialog <type>\n");
        return 0;
    }

    if (String(argv[1]) == "--shutdown")
        return run_shutdown_dialog(argc, argv);

    fprintf(stderr, "Unknown argument: %s\n", argv[1]);
    return 1;
}

int run_shutdown_dialog(int argc, char** argv)
{
    GApplication app(argc, argv);

    {
        GMessageBox box("Shut down Serenity?", "Confirm Shutdown", GMessageBox::Type::Warning, GMessageBox::InputType::OKCancel);
        auto result = box.exec();

        if (result == GMessageBox::ExecOK) {
            dbg() << "OK";
            int rc = execl("/bin/shutdown", "/bin/shutdown", "-n", nullptr);
            if (rc < 0) {
                perror("execl");
                return 1;
            }
        } else {
            dbg() << "Cancel";
            return 0;
        }
    }

    return app.exec();
}
