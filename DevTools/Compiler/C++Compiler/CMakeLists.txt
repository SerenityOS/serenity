set(SOURCES
    main.cpp
)

add_subdirectory(LibCpp)

serenity_bin(C++Compiler)

target_link_libraries(C++Compiler LibCpp LibMiddleEnd LibBackEnd LibCore)

