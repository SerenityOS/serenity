set(SOURCES
        FTPConnection.cpp
        FTPRequest.cpp
        FTPResponse.cpp
        FTPJob.cpp)

serenity_lib(LibFTP ftp)
target_link_libraries(LibFTP LibCore)
