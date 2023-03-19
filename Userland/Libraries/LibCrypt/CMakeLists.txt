
# HACK ALERT!
# To avoid a circular dependency chain with LibCrypt --> LibCrypto --> LibCore --> LibCrypt
# We include the SHA2 implementation from LibCrypto here manually
add_library(LibCryptSHA2 OBJECT ../LibCrypto/Hash/SHA2.cpp)
set_target_properties(LibCryptSHA2 PROPERTIES CXX_VISIBILITY_PRESET hidden)
set_target_properties(LibCryptSHA2 PROPERTIES VISIBILITY_INLINES_HIDDEN ON)

set(SOURCES
    crypt.cpp
)

serenity_lib(LibCrypt crypt)
serenity_install_headers("")
target_link_libraries(LibCrypt PRIVATE LibCryptSHA2)
