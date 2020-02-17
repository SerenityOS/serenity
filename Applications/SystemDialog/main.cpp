/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
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
    GUI::Application app(argc, argv);

    {
        auto result = GUI::MessageBox::show("Shut down Serenity?", "Confirm Shutdown", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNo);

        if (result == GUI::MessageBox::ExecYes) {
            int rc = execl("/bin/shutdown", "/bin/shutdown", "-n", nullptr);
            if (rc < 0) {
                perror("execl");
                return 1;
            }
            ASSERT_NOT_REACHED();
        }

        return 0;
    }
}
