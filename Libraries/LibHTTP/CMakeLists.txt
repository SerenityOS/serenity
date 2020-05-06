set(SOURCES
    HttpJob.cpp
    HttpRequest.cpp
    HttpResponse.cpp
    HttpsJob.cpp
    Job.cpp
)

serenity_lib(LibHTTP http)
target_link_libraries(LibHTTP LibCore LibTLS)
