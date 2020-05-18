set(SOURCES
        FTPConnection.cpp
        FTPRequest.cpp
        FTPResponse.cpp)

serenity_lib(LibFTP ftp)
target_link_libraries(LibFTP LibCore)
