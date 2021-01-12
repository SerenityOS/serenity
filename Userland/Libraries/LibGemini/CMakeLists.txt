set(SOURCES
    Document.cpp
    GeminiJob.cpp
    GeminiRequest.cpp
    GeminiResponse.cpp
    Job.cpp
    Line.cpp
)

serenity_lib(LibGemini gemini)
target_link_libraries(LibGemini LibCore LibTLS)
