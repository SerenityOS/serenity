set(SOURCES
    main.cpp
)

add_executable(ProtoBufCompiler ${SOURCES})
target_link_libraries(ProtoBufCompiler LagomCore)
