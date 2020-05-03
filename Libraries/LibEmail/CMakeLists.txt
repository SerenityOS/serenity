set(SOURCES
        IMAPClient.cpp
            Message.cpp)

    serenity_lib(LibEmail email)
        target_link_libraries(LibEmail LibC LibCore LibThread)
