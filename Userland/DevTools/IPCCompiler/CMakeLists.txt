set(SOURCES
    main.cpp
)

add_executable(IPCCompiler ${SOURCES})
target_link_libraries(IPCCompiler LagomCore)
