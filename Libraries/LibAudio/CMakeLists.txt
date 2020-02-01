set(SOURCES
    AClientConnection.cpp
    AWavLoader.cpp
)

serenity_lib(LibAudio audio)
add_dependencies(LibAudio AudioServer)
target_link_libraries(LibAudio LibIPC LibCore)
target_include_directories(LibAudio PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/../../Servers)
