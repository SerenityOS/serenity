

#include <AK/Format.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibMedia/Manip.h>
#include <LibMedia/readers/AVI/AVI.h>

using Media::u32_to_fourcc;

int main(const int argc, char** argv)
{
    String filePath;
    bool index = false;
    bool verbose = false;
    bool very_verbose = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display information about an audio/video file");
    args_parser.add_option(index, "Index tracks", "index", 'i');
    args_parser.add_option(verbose, "Verbose output", "verbose", 'v');
    args_parser.add_option(very_verbose, "Very verbose output", "VeryVerbose", 'V');
    args_parser.add_positional_argument(filePath, "Path to file", "path");
    args_parser.parse(argc, argv);

    // FIXME: get the reader type dynamically instead of hardcoding.
    Media::Reader::AVI::AVIReader reader(filePath);
    if (reader.is_open() == false) {
        warnln("Failed to open file");
        return 2;
    }

    if (very_verbose == true) {
        verbose = true;
    }

    outln("General");
    outln("Complete name\t\t: {}", filePath);
    outln("Format\t\t\t: {}", reader.format());
    outln("File Size\t\t: {:0.3} KiB", static_cast<double>(reader.size()) / KiB);
    outln("Duration\t\t: {:0.3} s", reader.duration());
    outln("Track Count\t\t: {}", reader.track_count());
    if (very_verbose) {
        // FIXME: dump out structure of data. e.g. track->dump();
    }
    outln("");

    for (u32 ix = 0; ix < reader.video_count(); ++ix) {
        outln("Video");
        auto const track = reader.video_track(ix);
        assert(track.ptr() != nullptr);
        outln("Codec ID\t\t: {}", u32_to_fourcc(track->codec()));
        outln("Duration\t\t: {:0.3} s", track->duration());
        outln("Width\t\t\t: {} px", track->dimensions().get_with_index<u32, 0>());
        outln("Height\t\t\t: {} px", track->dimensions().get_with_index<u32, 1>());
        outln("Display aspect ratio\t: {:0.3} ({})", track->frame_aspect_ratio().to_double(), track->frame_aspect_ratio().to_string());
        outln("Frame Rate\t\t: {:0.3} fps ({})", track->framerate().to_double(), track->framerate().to_string());
        outln("Stream size\t\t: {:0.3} KiB", static_cast<double>(track->size()) / KiB);
        if (very_verbose) {
            // FIXME: dump out structure of data. e.g. track->dump();
        }

        outln("");
    }

    for (u32 ix = 0; ix < reader.audio_count(); ++ix) {
        outln("Audio");
        auto const track = reader.audio_track(ix);
        assert(track.ptr() != nullptr);
        outln("Codec ID\t\t: {}", u32_to_fourcc(track->codec()));
        outln("Duration\t\t: {:0.3} s", track->duration());
        outln("Channel(s)\t\t: {}", track->channel_count());
        outln("Sampling rate\t\t: {} Hz", track->samplerate());
        outln("Stream size\t\t: {:0.3} KiB", static_cast<double>(track->size()) / KiB);
        if (very_verbose) {
            // FIXME: dump out structure of data. e.g. track->dump() to print out structure NALs or PESs;
        }

        outln("");
    }

    if (index == true) {
        outln("");
        outln("Indexed Samples");
        outln("");

        for (u32 ix = 0; ix < reader.video_count(); ++ix) {
            auto track = reader.video_track(ix);
            outln("Video Track #{}", ix);
            for (u32 count = 0; count < track->sample_count(); ++count) {
                auto sample = track->sample(count);
                if (sample.ptr() == nullptr) {
                    break;
                }
                // FIXME: use a decompressor/packet-parser
                out("[{:06}]\tsize: {: 6}", sample->index(), sample->size());
                if (verbose == true) {
                    out(", offset: 0x{:X}", sample->offset());
                }
                outln("");
            }
            outln("");
        }

        for (u32 ix = 0; ix < reader.audio_count(); ++ix) {
            auto track = reader.audio_track(ix);
            outln("Audio Track #{}", ix);
            for (u32 count = 0; count < track->sample_count(); ++count) {
                auto sample = track->sample(count);
                if (sample.ptr() == nullptr) {
                    break;
                }
                // FIXME: use a decompressor/packet-parser
                out("[{:06}]\tsize: {: 6}", sample->index(), sample->size());
                if (verbose == true) {
                    out(", offset: 0x{:X}", sample->offset());
                }
                outln("");
            }
            outln("");
        }

        for (u32 ix = 0; ix < reader.subtitle_count(); ++ix) {
            auto track = reader.subtitle_track(ix);
            outln("Subtitle Track #{}", ix);
            for (u32 count = 0; count < track->sample_count(); ++count) {
                auto sample = track->sample(count);
                if (sample.ptr() == nullptr) {
                    break;
                }
                // FIXME: use a decompressor/packet-parser
                out("[{:06}]\tsize: {: 6}", sample->index(), sample->size());
                if (verbose == true) {
                    out(", offset: 0x{:X}", sample->offset());
                }
                outln("");
            }
            outln("");
        }
    }

    return 0;
}