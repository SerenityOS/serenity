set(SOURCES
    crypt.cpp
)

serenity_libc(LibCrypt crypt)
target_link_libraries(LibCrypt LibC LibCrypto)
