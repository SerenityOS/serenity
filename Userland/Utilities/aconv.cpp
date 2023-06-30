/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Types.h>
#include <LibAudio/Loader.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <stdio.h>

static ErrorOr<StringView> guess_format_from_extension(StringView path)
{
    if (path == "-"sv)
        return Error::from_string_view("Cannot guess format for standard stream, please specify format manually"sv);

    LexicalPath lexical_path { path };
    auto extension = lexical_path.extension();
    if (extension.is_empty())
        return Error::from_string_view("Cannot guess format for file without file extension"sv);

    // Note: Do not return the `extension` StringView in any case, since that will possibly lead to UAF.
    if (extension == "wav"sv || extension == "wave"sv)
        return "wav"sv;
    if (extension == "flac"sv)
        return "flac"sv;
    if (extension == "mp3"sv || extension == "mpeg3"sv)
        return "mp3"sv;
    if (extension == "qoa"sv)
        return "qoa"sv;

    return Error::from_string_view("Cannot guess format for the given file extension"sv);
}

static ErrorOr<Audio::PcmSampleFormat> parse_sample_format(StringView textual_format)
{
    if (textual_format == "u8"sv)
        return Audio::PcmSampleFormat::Uint8;
    if (textual_format == "s16le"sv)
        return Audio::PcmSampleFormat::Int16;
    if (textual_format == "s24le"sv)
        return Audio::PcmSampleFormat::Int24;
    if (textual_format == "s32le"sv)
        return Audio::PcmSampleFormat::Int32;
    if (textual_format == "f32le"sv)
        return Audio::PcmSampleFormat::Float32;
    if (textual_format == "f64le"sv)
        return Audio::PcmSampleFormat::Float64;
    return Error::from_string_view("Unknown sample format"sv);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    StringView input {};
    StringView output {};
    StringView input_format {};
    StringView output_format {};
    StringView output_sample_format;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Convert between audio formats");
    args_parser.add_option(input, "Audio file to convert (or '-' for standard input)", "input", 'i', "input");
    args_parser.add_option(input_format, "Force input codec and container (see manual for supported codecs and containers)", "input-audio-codec", 0, "input-codec");
    args_parser.add_option(output_format, "Set output codec", "audio-codec", 0, "output-codec");
    args_parser.add_option(output_sample_format, "Set output sample format (see manual for supported formats)", "audio-format", 0, "sample-format");
    args_parser.add_option(output, "Target file (or '-' for standard output)", "output", 'o', "output");
    args_parser.parse(arguments);

    if (input.is_empty())
        return Error::from_string_view("Input file is required, use '-' to read from standard input"sv);

    if (output_format.is_empty() && output == "-"sv)
        return Error::from_string_view("Output format must be specified manually when writing to standard output"sv);

    if (input != "-"sv)
        TRY(Core::System::unveil(TRY(FileSystem::absolute_path(input)), "r"sv));
    if (output != "-"sv)
        TRY(Core::System::unveil(TRY(FileSystem::absolute_path(output)), "rwc"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    RefPtr<Audio::Loader> input_loader;
    // Use normal loader infrastructure to guess input format.
    if (input_format.is_empty()) {
        auto loader_or_error = Audio::Loader::create(input);
        if (loader_or_error.is_error()) {
            warnln("Could not guess codec for input file '{}'. Try forcing a codec with '--i:c:a'", input);
            return 1;
        }
        input_loader = loader_or_error.release_value();
    } else {
        warnln("Forcing input codec is not supported");
        return 1;
    }
    VERIFY(input_loader);

    if (output_format.is_empty())
        output_format = TRY(guess_format_from_extension(output));
    VERIFY(!output_format.is_empty());

    if (output_format == "wav"sv) {
        Optional<NonnullOwnPtr<Audio::WavWriter>> writer;
        if (!output.is_empty()) {
            auto parsed_output_sample_format = input_loader->pcm_format();
            if (!output_sample_format.is_empty())
                parsed_output_sample_format = TRY(parse_sample_format(output_sample_format));

            writer.emplace(TRY(Audio::WavWriter::create_from_file(
                output,
                static_cast<int>(input_loader->sample_rate()),
                input_loader->num_channels(),
                parsed_output_sample_format)));
        }
        if (output != "-"sv)
            out("Writing: \033[s");

        while (input_loader->loaded_samples() < input_loader->total_samples()) {
            auto samples_or_error = input_loader->get_more_samples();
            if (samples_or_error.is_error()) {
                warnln("Error while loading samples: {} (at {})", samples_or_error.error().description, samples_or_error.error().index);
                return 1;
            }
            auto samples = samples_or_error.release_value();
            if (writer.has_value())
                TRY((*writer)->write_samples(samples.span()));
            // TODO: Show progress updates like aplay by moving the progress calculation into a common utility function.
            if (output != "-"sv) {
                out("\033[u{}/{}", input_loader->loaded_samples(), input_loader->total_samples());
                fflush(stdout);
            }
        }

        if (writer.has_value())
            TRY((*writer)->finalize());
        if (output != "-"sv)
            outln();
    } else {
        warnln("Codec {} is not supported for encoding", output_format);
    }

    return 0;
}
