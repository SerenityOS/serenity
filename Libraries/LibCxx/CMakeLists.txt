set(SOURCES
    CxaGuard.cpp
)

# Shhh don't worry gcc, we are totally libstdc++
serenity_libc(LibCxx stdc++)
target_link_libraries(LibCxx LibC)
