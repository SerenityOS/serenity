set(SOURCES
    Header.cpp
    HeaderList.cpp
    HttpJob.cpp
    HttpRequest.cpp
    HttpResponse.cpp
    HttpsJob.cpp
    Job.cpp
)

serenity_lib(LibHTTP http)
target_link_libraries(LibHTTP LibCompress LibCore LibTLS)
