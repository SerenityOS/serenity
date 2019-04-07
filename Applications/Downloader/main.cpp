#include <LibGUI/GApplication.h>
#include <LibGUI/GHttpRequest.h>
#include <LibGUI/GHttpResponse.h>
#include <LibGUI/GNetworkJob.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    GHttpRequest request;
    request.set_hostname("www.google.com");
    request.set_path("/");

    auto job = request.schedule();
    job->on_finish = [&job] (bool success) {
        if (!success) {
            dbgprintf("on_finish: request failed :(\n");
            return;
        }
        auto& response = static_cast<const GHttpResponse&>(*job->response());
        printf("on_receive: code=%d\n", response.code());
        printf("payload:\n");
        printf("%s", response.payload().pointer());
        printf("payload was %d bytes\n", response.payload().size());
    };

    printf("Entering main loop...\n");
    return app.exec();
}
