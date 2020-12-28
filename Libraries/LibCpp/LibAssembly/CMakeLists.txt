set(SOURCES
        I386Assembly.cpp
)

serenity_lib(LibAssembly BackEnd)
target_link_libraries(LibAssembly LibCore)
