set(SOURCES
    Connection.cpp
    Decoder.cpp
    Encoder.cpp
    Message.cpp
)

serenity_lib(LibIPC ipc)
target_link_libraries(LibIPC PRIVATE LibCore LibURL)

# ===== BuggieBox_LibIPC for BuggieBox =====
add_library(BuggieBox_LibIPC STATIC ${SOURCES})
target_link_libraries(BuggieBox_LibIPC
    PUBLIC StaticPIE_CompileOptions
    PRIVATE BuggieBox_LibCore BuggieBox_LibURL
)
