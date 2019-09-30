#include <LibCore/CHttpRequest.h>
#include <LibCore/CHttpResponse.h>
#include <LibCore/CNetworkJob.h>
#include <LibGUI/GApplication.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    CHttpRequest request;
    request.set_url("http://www.google.com/");

    auto job = request.schedule();
    job->on_finish = [&job](bool success) {
        if (!success) {
            dbgprintf("on_finish: request failed :(\n");
            return;
        }
        auto& response = static_cast<const CHttpResponse&>(*job->response());
        printf("%s{%p}: on_receive: code=%d\n", job->class_name(), job.ptr(), response.code());
        //printf("payload:\n");
        //printf("%s", response.payload().data());
        printf("payload was %d bytes\n", response.payload().size());
    };

    printf("Entering main loop...\n");
    return app.exec();
}
