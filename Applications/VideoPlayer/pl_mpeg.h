/*
PL_MPEG - MPEG1 Video decoder, MP2 Audio decoder, MPEG-PS demuxer

Dominic Szablewski - https://phoboslab.org


-- LICENSE: The MIT License(MIT)

Copyright(c) 2019 Dominic Szablewski

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files(the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and / or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions :
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.




-- Synopsis

// Define `PL_MPEG_IMPLEMENTATION` in *one* C/C++ file before including this
// library to create the implementation.

#define PL_MPEG_IMPLEMENTATION
#include "plmpeg.h"

// This function gets called for each decoded video frame
void my_video_callback(plm_t *plm, plm_frame_t *frame, void *user) {
	// Do something with frame->y.data, frame->cr.data, frame->cb.data
}

// This function gets called for each decoded audio frame
void my_audio_callback(plm_t *plm, plm_samples_t *frame, void *user) {
	// Do something with samples->interleaved
}

// Load a .mpg (MPEG Program Stream) file
plm_t *plm = plm_create_with_filename("some-file.mpg");

// Install the video & audio decode callbacks
plm_set_video_decode_callback(plm, my_video_callback, my_data);
plm_set_audio_decode_callback(plm, my_audio_callback, my_data);


// Decode
do {
	plm_decode(plm, time_since_last_call);
} while (!plm_has_ended(plm));

// All done
plm_destroy(plm);



-- Documentation

This library provides several interfaces to load, demux and decode MPEG video
and audio data. A high-level API combines the demuxer, video & audio decoders
in an easy to use wrapper.

Lower-level APIs for accessing the demuxer, video decoder and audio decoder, 
as well as providing different data sources are also available.

Interfaces are written in an object orientet style, meaning you create object 
instances via various different constructor functions (plm_*create()),
do some work on them and later dispose them via plm_*destroy().

plm_* ......... the high-level interface, combining demuxer and decoders
plm_buffer_* .. the data source used by all interfaces
plm_demux_* ... the MPEG-PS demuxer
plm_video_* ... the MPEG1 Video ("mpeg1") decoder
plm_audio_* ... the MPEG1 Audio Layer II ("mp2") decoder


With the high-level interface you have two options to decode video & audio:

 1. Use plm_decode() and just hand over the delta time since the last call.
    It will decode everything needed and call your callbacks (specified through
    plm_set_{video|audio}_decode_callback()) any number of times.

 2. Use plm_decode_video() and plm_decode_audio() to decode exactly one
    frame of video or audio data at a time. How you handle the synchronization 
    of both streams is up to you.

If you only want to decode video *or* audio through these functions, you should
disable the other stream (plm_set_{video|audio}_enabled(FALSE))

Video data is decoded into a struct with all 3 planes (Y, Cr, Cb) stored in
separate buffers. You can either convert this to RGB on the CPU (slow) via the
plm_frame_to_rgb() function or do it on the GPU with the following matrix:

mat4 rec601 = mat4(
	1.16438,  0.00000,  1.59603, -0.87079,
	1.16438, -0.39176, -0.81297,  0.52959,
	1.16438,  2.01723,  0.00000, -1.08139,
	0, 0, 0, 1
);
gl_FragColor = vec4(y, cb, cr, 1.0) * rec601;

Audio data is decoded into a struct with either one single float array with the
samples for the left and right channel interleaved, or if the 
PLM_AUDIO_SEPARATE_CHANNELS is defined *before* including this library, into
two separate float arrays - one for each channel.


Data can be supplied to the high level interface, the demuxer and the decoders
in three different ways:

 1. Using plm_create_from_filename() or with a file handle with 
    plm_create_from_file().

 2. Using plm_create_with_memory() and supplying a pointer to memory that
    contains the whole file.

 3. Using plm_create_with_buffer(), supplying your own plm_buffer_t instance and
    periodically writing to this buffer.

When using your own plm_buffer_t instance, you can fill this buffer using 
plm_buffer_write(). You can either monitor plm_buffer_get_remaining() and push 
data when appropriate, or install a callback on the buffer with 
plm_buffer_set_load_callback() that gets called whenever the buffer needs more 
data.

A buffer created with plm_buffer_create_with_capacity() is treated as a ring
buffer, meaning that data that has already been read, will be discarded. In
contrast, a buffer created with plm_buffer_create_for_appending() will keep all
data written to it in memory. This enables seeking in the already loaded data.


There should be no need to use the lower level plm_demux_*, plm_video_* and 
plm_audio_* functions, if all you want to do is read/decode an MPEG-PS file.
However, if you get raw mpeg1video data or raw mp2 audio data from a different
source, these functions can be used to decode the raw data directly. Similarly, 
if you only want to analyze an MPEG-PS file or extract raw video or audio
packets from it, you can use the plm_demux_* functions.


This library uses malloc(), realloc() and free() to manage memory. Typically 
all allocation happens up-front when creating the interface. However, the
default buffer size may be too small for certain inputs. In these cases plmpeg
will realloc() the buffer with a larger size whenever needed. You can configure
the default buffer size by defining PLM_BUFFER_DEFAULT_SIZE *before* 
including this library.


See below for detailed the API documentation.

*/


#ifndef PL_MPEG_H
#define PL_MPEG_H

#include <stdint.h>
#include <stdio.h>


#ifdef __cplusplus
extern "C" {
#endif

// -----------------------------------------------------------------------------
// Public Data Types


// Object types for the various interfaces

typedef struct plm_t plm_t;
typedef struct plm_buffer_t plm_buffer_t;
typedef struct plm_demux_t plm_demux_t;
typedef struct plm_video_t plm_video_t;
typedef struct plm_audio_t plm_audio_t;


// Demuxed MPEG PS packet
// The type maps directly to the various MPEG-PES start codes. PTS is the
// presentation time stamp of the packet in seconds. Note that not all packets
// have a PTS value, indicated by PLM_PACKET_INVALID_TS.

#define PLM_PACKET_INVALID_TS -1

typedef struct {
	int type;
	double pts;
	size_t length;
	uint8_t *data;
} plm_packet_t;


// Decoded Video Plane 
// The byte length of the data is width * height. Note that different planes
// have different sizes: the Luma plane (Y) is double the size of each of 
// the two Chroma planes (Cr, Cb) - i.e. 4 times the byte length.
// Also note that the size of the plane does *not* denote the size of the 
// displayed frame. The sizes of planes are always rounded up to the nearest
// macroblock (16px).

typedef struct {
	unsigned int width;
	unsigned int height;
	uint8_t *data;
} plm_plane_t;


// Decoded Video Frame
// width and height denote the desired display size of the frame. This may be
// different from the internal size of the 3 planes.

typedef struct {
	double time;
	unsigned int width;
	unsigned int height;
	plm_plane_t y;
	plm_plane_t cr;
	plm_plane_t cb;
} plm_frame_t;


// Callback function type for decoded video frames used by the high-level
// plm_* interface

typedef void(*plm_video_decode_callback)
	(plm_t *self, plm_frame_t *frame, void *user);


// Decoded Audio Samples
// Samples are stored as normalized (-1, 1) float either interleaved, or if
// PLM_AUDIO_SEPARATE_CHANNELS is defined, in two separate arrays.
// The `count` is always PLM_AUDIO_SAMPLES_PER_FRAME and just there for
// convenience.

#define PLM_AUDIO_SAMPLES_PER_FRAME 1152

typedef struct {
	double time;
	unsigned int count;
	#ifdef PLM_AUDIO_SEPARATE_CHANNELS
		float left[PLM_AUDIO_SAMPLES_PER_FRAME];
		float right[PLM_AUDIO_SAMPLES_PER_FRAME];
	#else
		float interleaved[PLM_AUDIO_SAMPLES_PER_FRAME * 2];
	#endif
} plm_samples_t;


// Callback function type for decoded audio samples used by the high-level
// plm_* interface

typedef void(*plm_audio_decode_callback)
	(plm_t *self, plm_samples_t *samples, void *user);


// Callback function for plm_buffer when it needs more data

typedef void(*plm_buffer_load_callback)(plm_buffer_t *self, void *user);



// -----------------------------------------------------------------------------
// plm_* public API
// High-Level API for loading/demuxing/decoding MPEG-PS data


// Create a plmpeg instance with a filename. Returns NULL if the file could not
// be opened.

plm_t *plm_create_with_filename(const char *filename);


// Create a plmpeg instance with a file handle. Pass TRUE to close_when_done to
// let plmpeg call fclose() on the handle when plm_destroy() is called.

plm_t *plm_create_with_file(FILE *fh, int close_when_done);


// Create a plmpeg instance with a pointer to memory as source. This assumes the
// whole file is in memory. The memory is not copied. Pass TRUE to 
// free_when_done to let plmpeg call free() on the pointer when plm_destroy() 
// is called.

plm_t *plm_create_with_memory(uint8_t *bytes, size_t length, int free_when_done);


// Create a plmpeg instance with a plm_buffer as source. Pass TRUE to
// destroy_when_done to let plmpeg call plm_buffer_destroy() on the buffer when
// plm_destroy() is called.

plm_t *plm_create_with_buffer(plm_buffer_t *buffer, int destroy_when_done);


// Destroy a plmpeg instance and free all data.

void plm_destroy(plm_t *self);


// Get whether we have headers on all available streams and we can accurately
// report the number of video/audio streams, video dimensions, framerate and
// audio samplerate.
// This returns FALSE if the file is not an MPEG-PS file or - when not using a
// file as source - when not enough data is available yet.

int plm_has_headers(plm_t *self);


// Get or set whether video decoding is enabled. Default TRUE.

int plm_get_video_enabled(plm_t *self);
void plm_set_video_enabled(plm_t *self, int enabled);


// Get the number of video streams (0--1) reported in the system header.

int plm_get_num_video_streams(plm_t *self);


// Get the display width/height of the video stream.

int plm_get_width(plm_t *self);
int plm_get_height(plm_t *self);


// Get the framerate of the video stream in frames per second.

double plm_get_framerate(plm_t *self);


// Get or set whether audio decoding is enabled. Default TRUE.

int plm_get_audio_enabled(plm_t *self);
void plm_set_audio_enabled(plm_t *self, int enabled);


// Get the number of audio streams (0--4) reported in the system header.

int plm_get_num_audio_streams(plm_t *self);


// Set the desired audio stream (0--3). Default 0.

void plm_set_audio_stream(plm_t *self, int stream_index);


// Get the samplerate of the audio stream in samples per second.

int plm_get_samplerate(plm_t *self);


// Get or set the audio lead time in seconds - the time in which audio samples
// are decoded in advance (or behind) the video decode time. Typically this
// should be set to the duration of the buffer of the audio API that you use
// for output. E.g. for SDL2: (SDL_AudioSpec.samples / samplerate)

double plm_get_audio_lead_time(plm_t *self);
void plm_set_audio_lead_time(plm_t *self, double lead_time);


// Get the current internal time in seconds.

double plm_get_time(plm_t *self);


// Get the video duration of the underlying source in seconds.

double plm_get_duration(plm_t *self);


// Rewind all buffers back to the beginning.

void plm_rewind(plm_t *self);


// Get or set looping. Default FALSE.

int plm_get_loop(plm_t *self);
void plm_set_loop(plm_t *self, int loop);


// Get whether the file has ended. If looping is enabled, this will always
// return FALSE.

int plm_has_ended(plm_t *self);


// Set the callback for decoded video frames used with plm_decode(). If no 
// callback is set, video data will be ignored and not be decoded. The *user
// Parameter will be passed to your callback.

void plm_set_video_decode_callback(plm_t *self, plm_video_decode_callback fp, void *user);


// Set the callback for decoded audio samples used with plm_decode(). If no 
// callback is set, audio data will be ignored and not be decoded. The *user
// Parameter will be passed to your callback.

void plm_set_audio_decode_callback(plm_t *self, plm_audio_decode_callback fp, void *user);


// Advance the internal timer by seconds and decode video/audio up to this time.
// This will call the video_decode_callback and audio_decode_callback any number
// of times. A frame-skip is not implemented, i.e. everything up to current time
// will be decoded.

void plm_decode(plm_t *self, double seconds);


// Decode and return one video frame. Returns NULL if no frame could be decoded
// (either because the source ended or data is corrupt). If you only want to 
// decode video, you should disable audio via plm_set_audio_enabled().
// The returned plm_frame_t is valid until the next call to plm_decode_video() 
// or until plm_destroy() is called.

plm_frame_t *plm_decode_video(plm_t *self);


// Decode and return one audio frame. Returns NULL if no frame could be decoded
// (either because the source ended or data is corrupt). If you only want to 
// decode audio, you should disable video via plm_set_video_enabled().
// The returned plm_samples_t is valid until the next call to plm_decode_audio()
// or until plm_destroy() is called.

plm_samples_t *plm_decode_audio(plm_t *self);


// Seek to the specified time, clamped between 0 -- duration. This can only be 
// used when the underlying plm_buffer is seekable, i.e. for files, fixed 
// memory buffers or _for_appending buffers. 
// If seek_exact is TRUE this will seek to the exact time, otherwise it will 
// seek to the last intra frame just before the desired time. Exact seeking can 
// be slow, because all frames up to the seeked one have to be decoded on top of
// the previous intra frame.
// If seeking succeeds, this function will call the video_decode_callback 
// exactly once with the target frame. If audio is enabled, it will also call
// the audio_decode_callback any number of times, until the audio_lead_time is
// satisfied.
// Returns TRUE if seeking succeeded or FALSE if no frame could be found.

int plm_seek(plm_t *self, double time, int seek_exact);


// Similar to plm_seek(), but will not call the video_decode_callback,
// audio_decode_callback or make any attempts to sync audio.
// Returns the found frame or NULL if no frame could be found.

plm_frame_t *plm_seek_frame(plm_t *self, double time, int seek_exact);



// -----------------------------------------------------------------------------
// plm_buffer public API
// Provides the data source for all other plm_* interfaces


// The default size for buffers created from files or by the high-level API

#ifndef PLM_BUFFER_DEFAULT_SIZE
#define PLM_BUFFER_DEFAULT_SIZE (128 * 1024)
#endif


// Create a buffer instance with a filename. Returns NULL if the file could not
// be opened.

plm_buffer_t *plm_buffer_create_with_filename(const char *filename);


// Create a buffer instance with a file handle. Pass TRUE to close_when_done
// to let plmpeg call fclose() on the handle when plm_destroy() is called.

plm_buffer_t *plm_buffer_create_with_file(FILE *fh, int close_when_done);


// Create a buffer instance with a pointer to memory as source. This assumes
// the whole file is in memory. The bytes are not copied. Pass 1 to 
// free_when_done to let plmpeg call free() on the pointer when plm_destroy() 
// is called.

plm_buffer_t *plm_buffer_create_with_memory(uint8_t *bytes, size_t length, int free_when_done);


// Create an empty buffer with an initial capacity. The buffer will grow
// as needed. Data that has already been read, will be discarded.

plm_buffer_t *plm_buffer_create_with_capacity(size_t capacity);


// Create an empty buffer with an initial capacity. The buffer will grow
// as needed. Decoded data will *not* be discarded. This can be used when
// loading a file over the network, without needing to throttle the download. 
// It also allows for seeking in the already loaded data.

plm_buffer_t *plm_buffer_create_for_appending(size_t initial_capacity);


// Destroy a buffer instance and free all data

void plm_buffer_destroy(plm_buffer_t *self);


// Copy data into the buffer. If the data to be written is larger than the 
// available space, the buffer will realloc() with a larger capacity. 
// Returns the number of bytes written. This will always be the same as the
// passed in length, except when the buffer was created _with_memory() for
// which _write() is forbidden.

size_t plm_buffer_write(plm_buffer_t *self, uint8_t *bytes, size_t length);


// Mark the current byte length as the end of this buffer and signal that no 
// more data is expected to be written to it. This function should be called
// just after the last plm_buffer_write().
// For _with_capacity buffers, this is cleared on a plm_buffer_rewind().

void plm_buffer_signal_end(plm_buffer_t *self);


// Set a callback that is called whenever the buffer needs more data

void plm_buffer_set_load_callback(plm_buffer_t *self, plm_buffer_load_callback fp, void *user);


// Rewind the buffer back to the beginning. When loading from a file handle,
// this also seeks to the beginning of the file.

void plm_buffer_rewind(plm_buffer_t *self);


// Get the total size. For files, this returns the file size. For all other 
// types it returns the number of bytes currently in the buffer.

size_t plm_buffer_get_size(plm_buffer_t *self);


// Get the number of remaining (yet unread) bytes in the buffer. This can be
// useful to throttle writing.

size_t plm_buffer_get_remaining(plm_buffer_t *self);


// Get whether the read position of the buffer is at the end and no more data 
// is expected.

int plm_buffer_has_ended(plm_buffer_t *self);



// -----------------------------------------------------------------------------
// plm_demux public API
// Demux an MPEG Program Stream (PS) data into separate packages


// Various Packet Types

static const int PLM_DEMUX_PACKET_PRIVATE = 0xBD;
static const int PLM_DEMUX_PACKET_AUDIO_1 = 0xC0;
static const int PLM_DEMUX_PACKET_AUDIO_2 = 0xC1;
static const int PLM_DEMUX_PACKET_AUDIO_3 = 0xC2;
static const int PLM_DEMUX_PACKET_AUDIO_4 = 0xC2;
static const int PLM_DEMUX_PACKET_VIDEO_1 = 0xE0;


// Create a demuxer with a plm_buffer as source. This will also attempt to read
// the pack and system headers from the buffer.

plm_demux_t *plm_demux_create(plm_buffer_t *buffer, int destroy_when_done);


// Destroy a demuxer and free all data.

void plm_demux_destroy(plm_demux_t *self);


// Returns TRUE/FALSE whether pack and system headers have been found. This will
// attempt to read the headers if non are present yet.

int plm_demux_has_headers(plm_demux_t *self);


// Returns the number of video streams found in the system header. This will
// attempt to read the system header if non is present yet.

int plm_demux_get_num_video_streams(plm_demux_t *self);


// Returns the number of audio streams found in the system header. This will
// attempt to read the system header if non is present yet.

int plm_demux_get_num_audio_streams(plm_demux_t *self);


// Rewind the internal buffer. See plm_buffer_rewind().

void plm_demux_rewind(plm_demux_t *self);


// Get whether the file has ended. This will be cleared on seeking or rewind.

int plm_demux_has_ended(plm_demux_t *self);


// Seek to a packet of the specified type with a PTS just before specified time.
// If force_intra is TRUE, only packets containing an intra frame will be 
// considered - this only makes sense when the type is PLM_DEMUX_PACKET_VIDEO_1.
// Note that the specified time is considered 0-based, regardless of the first 
// PTS in the data source.

plm_packet_t *plm_demux_seek(plm_demux_t *self, double time, int type, int force_intra);


// Get the PTS of the first packet of this type. Returns PLM_PACKET_INVALID_TS
// if not packet of this packet type can be found.

double plm_demux_get_start_time(plm_demux_t *self, int type);


// Get the duration for the specified packet type - i.e. the span between the
// the first PTS and the last PTS in the data source. This only makes sense when
// the underlying data source is a file or fixed memory.

double plm_demux_get_duration(plm_demux_t *self, int type);


// Decode and return the next packet. The returned packet_t is valid until
// the next call to plm_demux_decode() or until the demuxer is destroyed.

plm_packet_t *plm_demux_decode(plm_demux_t *self);



// -----------------------------------------------------------------------------
// plm_video public API
// Decode MPEG1 Video ("mpeg1") data into raw YCrCb frames


// Create a video decoder with a plm_buffer as source.

plm_video_t *plm_video_create_with_buffer(plm_buffer_t *buffer, int destroy_when_done);


// Destroy a video decoder and free all data.

void plm_video_destroy(plm_video_t *self);


// Get whether a sequence header was found and we can accurately report on
// dimensions and framerate.

int plm_video_has_header(plm_video_t *self);


// Get the framerate in frames per second.

double plm_video_get_framerate(plm_video_t *self);


// Get the display width/height.

int plm_video_get_width(plm_video_t *self);
int plm_video_get_height(plm_video_t *self);


// Set "no delay" mode. When enabled, the decoder assumes that the video does
// *not* contain any B-Frames. This is useful for reducing lag when streaming.
// The default is FALSE.

void plm_video_set_no_delay(plm_video_t *self, int no_delay);


// Get the current internal time in seconds.

double plm_video_get_time(plm_video_t *self);


// Set the current internal time in seconds. This is only useful when you
// manipulate the underlying video buffer and want to enforce a correct
// timestamps.

void plm_video_set_time(plm_video_t *self, double time);


// Rewind the internal buffer. See plm_buffer_rewind().

void plm_video_rewind(plm_video_t *self);


// Get whether the file has ended. This will be cleared on rewind.

int plm_video_has_ended(plm_video_t *self);


// Decode and return one frame of video and advance the internal time by 
// 1/framerate seconds. The returned frame_t is valid until the next call of
// plm_video_decode() or until the video decoder is destroyed.

plm_frame_t *plm_video_decode(plm_video_t *self);


// Convert the YCrCb data of a frame into interleaved R G B data. The stride
// specifies the width in bytes of the destination buffer. I.e. the number of
// bytes from one line to the next. The stride must be at least 
// (frame->width * bytes_per_pixel). The buffer pointed to by *dest must have a
// size of at least (stride * frame->height).
// Note that the alpha component of the dest buffer is always left untouched.

void plm_frame_to_rgb(plm_frame_t *frame, uint8_t *dest, int stride);
void plm_frame_to_bgr(plm_frame_t *frame, uint8_t *dest, int stride);
void plm_frame_to_rgba(plm_frame_t *frame, uint8_t *dest, int stride);
void plm_frame_to_bgra(plm_frame_t *frame, uint8_t *dest, int stride);
void plm_frame_to_argb(plm_frame_t *frame, uint8_t *dest, int stride);
void plm_frame_to_abgr(plm_frame_t *frame, uint8_t *dest, int stride);


// -----------------------------------------------------------------------------
// plm_audio public API
// Decode MPEG-1 Audio Layer II ("mp2") data into raw samples


// Create an audio decoder with a plm_buffer as source.

plm_audio_t *plm_audio_create_with_buffer(plm_buffer_t *buffer, int destroy_when_done);


// Destroy an audio decoder and free all data.

void plm_audio_destroy(plm_audio_t *self);


// Get whether a frame header was found and we can accurately report on
// samplerate.

int plm_audio_has_header(plm_audio_t *self);


// Get the samplerate in samples per second.

int plm_audio_get_samplerate(plm_audio_t *self);


// Get the current internal time in seconds.

double plm_audio_get_time(plm_audio_t *self);


// Set the current internal time in seconds. This is only useful when you
// manipulate the underlying video buffer and want to enforce a correct
// timestamps.

void plm_audio_set_time(plm_audio_t *self, double time);


// Rewind the internal buffer. See plm_buffer_rewind().

void plm_audio_rewind(plm_audio_t *self);


// Get whether the file has ended. This will be cleared on rewind.

int plm_audio_has_ended(plm_audio_t *self);


// Decode and return one "frame" of audio and advance the internal time by 
// (PLM_AUDIO_SAMPLES_PER_FRAME/samplerate) seconds. The returned samples_t 
// is valid until the next call of plm_audio_decode() or until the audio
// decoder is destroyed.

plm_samples_t *plm_audio_decode(plm_audio_t *self);



#ifdef __cplusplus
}
#endif

#endif // PL_MPEG_H





// -----------------------------------------------------------------------------
// -----------------------------------------------------------------------------
// IMPLEMENTATION

#ifdef PL_MPEG_IMPLEMENTATION

#include <string.h>
#include <stdlib.h>

#ifndef TRUE
#define TRUE 1
#define FALSE 0
#endif

#define PLM_UNUSED(expr) (void)(expr)


// -----------------------------------------------------------------------------
// plm (high-level interface) implementation

typedef struct plm_t {
	plm_demux_t *demux;
	double time;
	int has_ended;
	int loop;
	int has_decoders;

	int video_enabled;
	int video_packet_type;
	plm_buffer_t *video_buffer;
	plm_video_t *video_decoder;

	int audio_enabled;
	int audio_stream_index;
	int audio_packet_type;
	double audio_lead_time;
	plm_buffer_t *audio_buffer;
	plm_audio_t *audio_decoder;

	plm_video_decode_callback video_decode_callback;
	void *video_decode_callback_user_data;

	plm_audio_decode_callback audio_decode_callback;
	void *audio_decode_callback_user_data;
} plm_t;

int plm_init_decoders(plm_t *self);
void plm_handle_end(plm_t *self);
void plm_read_video_packet(plm_buffer_t *buffer, void *user);
void plm_read_audio_packet(plm_buffer_t *buffer, void *user);
void plm_read_packets(plm_t *self, int requested_type);

plm_t *plm_create_with_filename(const char *filename) {
	plm_buffer_t *buffer = plm_buffer_create_with_filename(filename);
	if (!buffer) {
		return NULL;
	}
	return plm_create_with_buffer(buffer, TRUE);
}

plm_t *plm_create_with_file(FILE *fh, int close_when_done) {
	plm_buffer_t *buffer = plm_buffer_create_with_file(fh, close_when_done);
	return plm_create_with_buffer(buffer, TRUE);
}

plm_t *plm_create_with_memory(uint8_t *bytes, size_t length, int free_when_done) {
	plm_buffer_t *buffer = plm_buffer_create_with_memory(bytes, length, free_when_done);
	return plm_create_with_buffer(buffer, TRUE);
}

plm_t *plm_create_with_buffer(plm_buffer_t *buffer, int destroy_when_done) {
	plm_t *self = (plm_t *)malloc(sizeof(plm_t));
	memset(self, 0, sizeof(plm_t));

	self->demux = plm_demux_create(buffer, destroy_when_done);
	self->video_enabled = TRUE;
	self->audio_enabled = TRUE;
	plm_init_decoders(self);

	return self;
}

int plm_init_decoders(plm_t *self) {
	if (self->has_decoders) {
		return TRUE;
	}

	if (!plm_demux_has_headers(self->demux)) {
		return FALSE;
	}

	if (plm_demux_get_num_video_streams(self->demux) > 0) {
		if (self->video_enabled) {
			self->video_packet_type = PLM_DEMUX_PACKET_VIDEO_1;
		}
		self->video_buffer = plm_buffer_create_with_capacity(PLM_BUFFER_DEFAULT_SIZE);
		plm_buffer_set_load_callback(self->video_buffer, plm_read_video_packet, self);
	}

	if (plm_demux_get_num_audio_streams(self->demux) > 0) {
		if (self->audio_enabled) {
			self->audio_packet_type = PLM_DEMUX_PACKET_AUDIO_1 + self->audio_stream_index;
		}
		self->audio_buffer = plm_buffer_create_with_capacity(PLM_BUFFER_DEFAULT_SIZE);
		plm_buffer_set_load_callback(self->audio_buffer, plm_read_audio_packet, self);
	}

	if (self->video_buffer) {
		self->video_decoder = plm_video_create_with_buffer(self->video_buffer, TRUE);
	}

	if (self->audio_buffer) {
		self->audio_decoder = plm_audio_create_with_buffer(self->audio_buffer, TRUE);
	}

	self->has_decoders = TRUE;
	return TRUE;
}

void plm_destroy(plm_t *self) {
	if (self->video_decoder) {
		plm_video_destroy(self->video_decoder);
	}
	if (self->audio_decoder) {
		plm_audio_destroy(self->audio_decoder);
	}

	plm_demux_destroy(self->demux);
	free(self);
}

int plm_get_audio_enabled(plm_t *self) {
	return self->audio_enabled;
}

int plm_has_headers(plm_t *self) {
	if (!plm_demux_has_headers(self->demux)) {
		return FALSE;
	}
	
	if (!plm_init_decoders(self)) {
		return FALSE;
	}

	if (
		(self->video_decoder && !plm_video_has_header(self->video_decoder)) ||
		(self->audio_decoder && !plm_audio_has_header(self->audio_decoder))
	) {
		return FALSE;
	}

	return TRUE;
}

void plm_set_audio_enabled(plm_t *self, int enabled) {
	self->audio_enabled = enabled;

	if (!enabled) {
		self->audio_packet_type = 0;
		return;
	}

	self->audio_packet_type = (plm_init_decoders(self) && self->audio_decoder)
		? PLM_DEMUX_PACKET_AUDIO_1 + self->audio_stream_index
		: 0;
}

void plm_set_audio_stream(plm_t *self, int stream_index) {
	if (stream_index < 0 || stream_index > 3) {
		return;
	}
	self->audio_stream_index = stream_index;

	// Set the correct audio_packet_type
	plm_set_audio_enabled(self, self->audio_enabled);
}

int plm_get_video_enabled(plm_t *self) {
	return self->video_enabled;
}

void plm_set_video_enabled(plm_t *self, int enabled) {
	self->video_enabled = enabled;

	if (!enabled) {
		self->video_packet_type = 0;
		return;
	}

	self->video_packet_type = (plm_init_decoders(self) && self->video_decoder)
		? PLM_DEMUX_PACKET_VIDEO_1
		: 0;
}

int plm_get_num_video_streams(plm_t *self) {
	return plm_demux_get_num_video_streams(self->demux);
}

int plm_get_width(plm_t *self) {
	return (plm_init_decoders(self) && self->video_decoder)
		? plm_video_get_width(self->video_decoder)
		: 0;
}

int plm_get_height(plm_t *self) {
	return (plm_init_decoders(self) && self->video_decoder)
		? plm_video_get_height(self->video_decoder)
		: 0;
}

double plm_get_framerate(plm_t *self) {
	return (plm_init_decoders(self) && self->video_decoder)
		? plm_video_get_framerate(self->video_decoder)
		: 0;
}

int plm_get_num_audio_streams(plm_t *self) {
	return plm_demux_get_num_audio_streams(self->demux);
}

int plm_get_samplerate(plm_t *self) {
	return (plm_init_decoders(self) && self->audio_decoder)
		? plm_audio_get_samplerate(self->audio_decoder)
		: 0;
}

double plm_get_audio_lead_time(plm_t *self) {
	return self->audio_lead_time;
}

void plm_set_audio_lead_time(plm_t *self, double lead_time) {
	self->audio_lead_time = lead_time;
}

double plm_get_time(plm_t *self) {
	return self->time;
}

double plm_get_duration(plm_t *self) {
	return plm_demux_get_duration(self->demux, PLM_DEMUX_PACKET_VIDEO_1);
}

void plm_rewind(plm_t *self) {
	if (self->video_decoder) {
		plm_video_rewind(self->video_decoder);
	}

	if (self->audio_decoder) {
		plm_audio_rewind(self->audio_decoder);
	}

	plm_demux_rewind(self->demux);
	self->time = 0;
}

int plm_get_loop(plm_t *self) {
	return self->loop;
}

void plm_set_loop(plm_t *self, int loop) {
	self->loop = loop;
}

int plm_has_ended(plm_t *self) {
	return self->has_ended;
}

void plm_set_video_decode_callback(plm_t *self, plm_video_decode_callback fp, void *user) {
	self->video_decode_callback = fp;
	self->video_decode_callback_user_data = user;
}

void plm_set_audio_decode_callback(plm_t *self, plm_audio_decode_callback fp, void *user) {
	self->audio_decode_callback = fp;
	self->audio_decode_callback_user_data = user;
}

void plm_decode(plm_t *self, double tick) {
	if (!plm_init_decoders(self)) {
		return;
	}

	int decode_video = (self->video_decode_callback && self->video_packet_type);
	int decode_audio = (self->audio_decode_callback && self->audio_packet_type);

	if (!decode_video && !decode_audio) {
		// Nothing to do here
		return;
	}

	int did_decode = FALSE;
	int decode_video_failed = FALSE;
	int decode_audio_failed = FALSE;

	double video_target_time = self->time + tick;
	double audio_target_time = self->time + tick + self->audio_lead_time;

	do {
		did_decode = FALSE;
		
		if (decode_video && plm_video_get_time(self->video_decoder) < video_target_time) {
			plm_frame_t *frame = plm_video_decode(self->video_decoder);
			if (frame) {
				self->video_decode_callback(self, frame, self->video_decode_callback_user_data);
				did_decode = TRUE;
			}
			else {
				decode_video_failed = TRUE;
			}
		}

		if (decode_audio && plm_audio_get_time(self->audio_decoder) < audio_target_time) {
			plm_samples_t *samples = plm_audio_decode(self->audio_decoder);
			if (samples) {
				self->audio_decode_callback(self, samples, self->audio_decode_callback_user_data);
				did_decode = TRUE;
			}
			else {
				decode_audio_failed = TRUE;
			}
		}
	} while (did_decode);
	
	// Did all sources we wanted to decode fail and the demuxer is at the end?
	if (
		(!decode_video || decode_video_failed) && 
		(!decode_audio || decode_audio_failed) &&
		plm_demux_has_ended(self->demux)
	) {
		plm_handle_end(self);
		return;
	}

	self->time += tick;
}

plm_frame_t *plm_decode_video(plm_t *self) {
	if (!plm_init_decoders(self)) {
		return NULL;
	}

	if (!self->video_packet_type) {
		return NULL;
	}

	plm_frame_t *frame = plm_video_decode(self->video_decoder);
	if (frame) {
		self->time = frame->time;
	}
	else if (plm_demux_has_ended(self->demux)) {
		plm_handle_end(self);
	}
	return frame;
}

plm_samples_t *plm_decode_audio(plm_t *self) {
	if (!plm_init_decoders(self)) {
		return NULL;
	}

	if (!self->audio_packet_type) {
		return NULL;
	}

	plm_samples_t *samples = plm_audio_decode(self->audio_decoder);
	if (samples) {
		self->time = samples->time;
	}
	else if (plm_demux_has_ended(self->demux)) {
		plm_handle_end(self);
	}
	return samples;
}

void plm_handle_end(plm_t *self) {
	if (self->loop) {
		plm_rewind(self);
	}
	else {
		self->has_ended = TRUE;
	}
}

void plm_read_video_packet(plm_buffer_t *buffer, void *user) {
	PLM_UNUSED(buffer);
	plm_t *self = (plm_t *)user;
	plm_read_packets(self, self->video_packet_type);
}

void plm_read_audio_packet(plm_buffer_t *buffer, void *user) {
	PLM_UNUSED(buffer);
	plm_t *self = (plm_t *)user;
	plm_read_packets(self, self->audio_packet_type);
}

void plm_read_packets(plm_t *self, int requested_type) {
	plm_packet_t *packet;
	while ((packet = plm_demux_decode(self->demux))) {
		if (packet->type == self->video_packet_type) {
			plm_buffer_write(self->video_buffer, packet->data, packet->length);
		}
		else if (packet->type == self->audio_packet_type) {
			plm_buffer_write(self->audio_buffer, packet->data, packet->length);
		}

		if (packet->type == requested_type) {
			return;
		}
	}

	if (plm_demux_has_ended(self->demux)) {
		if (self->video_buffer) {
			plm_buffer_signal_end(self->video_buffer);
		}
		if (self->audio_buffer) {
			plm_buffer_signal_end(self->audio_buffer);
		}
	}
}

plm_frame_t *plm_seek_frame(plm_t *self, double time, int seek_exact) {
	if (!plm_init_decoders(self)) {
		return NULL;
	}

	if (!self->video_packet_type) {
		return NULL;
	}

	int type = self->video_packet_type;

	double start_time = plm_demux_get_start_time(self->demux, type);
	double duration = plm_demux_get_duration(self->demux, type);

	if (time < 0) {
		time = 0;
	}
	else if (time > duration) {
		time = duration;
	}
	
	plm_packet_t *packet = plm_demux_seek(self->demux, time, type, TRUE);
	if (!packet) {
		return NULL;
	}

	// Disable writing to the audio buffer while decoding video
	int previous_audio_packet_type = self->audio_packet_type;
	self->audio_packet_type = 0;

	// Clear video buffer and decode the found packet
	plm_video_rewind(self->video_decoder);
	plm_video_set_time(self->video_decoder, packet->pts - start_time);
	plm_buffer_write(self->video_buffer, packet->data, packet->length);
	plm_frame_t *frame = plm_video_decode(self->video_decoder);	

	// If we want to seek to an exact frame, we have to decode all frames
	// on top of the intra frame we just jumped to.
	if (seek_exact) {
		while (frame && frame->time < time) {
			frame = plm_video_decode(self->video_decoder);
		}
	}

	// Enable writing to the audio buffer again?
	self->audio_packet_type = previous_audio_packet_type;

	if (frame) {
		self->time = frame->time;
	}

	self->has_ended = FALSE;
	return frame;
}

int plm_seek(plm_t *self, double time, int seek_exact) {
	plm_frame_t *frame = plm_seek_frame(self, time, seek_exact);
	
	if (!frame) {
		return FALSE;
	}

	if (self->video_decode_callback) {
		self->video_decode_callback(self, frame, self->video_decode_callback_user_data);	
	}

	// If audio is not enabled we are done here.
	if (!self->audio_packet_type) {
		return TRUE;
	}

	// Sync up Audio. This demuxes more packets until the first audio packet
	// with a PTS greater than the current time is found. plm_decode() is then
	// called to decode enough audio data to satisfy the audio_lead_time.

	double start_time = plm_demux_get_start_time(self->demux, self->video_packet_type);
	plm_audio_rewind(self->audio_decoder);

	plm_packet_t *packet = NULL;
	while ((packet = plm_demux_decode(self->demux))) {
		if (packet->type == self->video_packet_type) {
			plm_buffer_write(self->video_buffer, packet->data, packet->length);
		}
		else if (
			packet->type == self->audio_packet_type &&
			packet->pts - start_time > self->time
		) {
			plm_audio_set_time(self->audio_decoder, packet->pts - start_time);
			plm_buffer_write(self->audio_buffer, packet->data, packet->length);
			plm_decode(self, 0);
			break;
		}
	}	
	
	return TRUE;
}



// -----------------------------------------------------------------------------
// plm_buffer implementation

enum plm_buffer_mode {
	PLM_BUFFER_MODE_FILE,
	PLM_BUFFER_MODE_FIXED_MEM,
	PLM_BUFFER_MODE_RING,
	PLM_BUFFER_MODE_APPEND
};

typedef struct plm_buffer_t {
	size_t bit_index;
	size_t capacity;
	size_t length;
	size_t total_size;
	int discard_read_bytes;
	int has_ended;
	int free_when_done;
	int close_when_done;
	FILE *fh;
	plm_buffer_load_callback load_callback;
	void *load_callback_user_data;
	uint8_t *bytes;
	enum plm_buffer_mode mode;
} plm_buffer_t;

typedef struct {
	int16_t index;
	int16_t value;
} plm_vlc_t;

typedef struct {
	int16_t index;
	uint16_t value;
} plm_vlc_uint_t;


void plm_buffer_seek(plm_buffer_t *self, size_t pos);
size_t plm_buffer_tell(plm_buffer_t *self);
void plm_buffer_discard_read_bytes(plm_buffer_t *self);
void plm_buffer_load_file_callback(plm_buffer_t *self, void *user);

int plm_buffer_has(plm_buffer_t *self, size_t count);
int plm_buffer_read(plm_buffer_t *self, int count);
void plm_buffer_align(plm_buffer_t *self);
void plm_buffer_skip(plm_buffer_t *self, size_t count);
int plm_buffer_skip_bytes(plm_buffer_t *self, uint8_t v);
int plm_buffer_next_start_code(plm_buffer_t *self);
int plm_buffer_find_start_code(plm_buffer_t *self, int code);
int plm_buffer_no_start_code(plm_buffer_t *self);
int16_t plm_buffer_read_vlc(plm_buffer_t *self, const plm_vlc_t *table);
uint16_t plm_buffer_read_vlc_uint(plm_buffer_t *self, const plm_vlc_uint_t *table);

plm_buffer_t *plm_buffer_create_with_filename(const char *filename) {
	FILE *fh = fopen(filename, "rb");
	if (!fh) {
		return NULL;
	}
	return plm_buffer_create_with_file(fh, TRUE);
}

plm_buffer_t *plm_buffer_create_with_file(FILE *fh, int close_when_done) {
	plm_buffer_t *self = plm_buffer_create_with_capacity(PLM_BUFFER_DEFAULT_SIZE);
	self->fh = fh;
	self->close_when_done = close_when_done;
	self->mode = PLM_BUFFER_MODE_FILE;
	self->discard_read_bytes = TRUE;
	
	fseek(self->fh, 0, SEEK_END);
	self->total_size = ftell(self->fh);
	fseek(self->fh, 0, SEEK_SET);

	plm_buffer_set_load_callback(self, plm_buffer_load_file_callback, NULL);
	return self;
}

plm_buffer_t *plm_buffer_create_with_memory(uint8_t *bytes, size_t length, int free_when_done) {
	plm_buffer_t *self = (plm_buffer_t *)malloc(sizeof(plm_buffer_t));
	memset(self, 0, sizeof(plm_buffer_t));
	self->capacity = length;
	self->length = length;
	self->total_size = length;
	self->free_when_done = free_when_done;
	self->bytes = bytes;
	self->mode = PLM_BUFFER_MODE_FIXED_MEM;
	self->discard_read_bytes = FALSE;
	return self;
}

plm_buffer_t *plm_buffer_create_with_capacity(size_t capacity) {
	plm_buffer_t *self = (plm_buffer_t *)malloc(sizeof(plm_buffer_t));
	memset(self, 0, sizeof(plm_buffer_t));
	self->capacity = capacity;
	self->free_when_done = TRUE;
	self->bytes = (uint8_t *)malloc(capacity);
	self->mode = PLM_BUFFER_MODE_RING;
	self->discard_read_bytes = TRUE;
	return self;
}

plm_buffer_t *plm_buffer_create_for_appending(size_t initial_capacity) {
	plm_buffer_t *self = plm_buffer_create_with_capacity(initial_capacity);
	self->mode = PLM_BUFFER_MODE_APPEND;
	self->discard_read_bytes = FALSE;
	return self;
}

void plm_buffer_destroy(plm_buffer_t *self) {
	if (self->fh && self->close_when_done) {
		fclose(self->fh);
	}
	if (self->free_when_done) {
		free(self->bytes);
	}
	free(self);
}

size_t plm_buffer_get_size(plm_buffer_t *self) {
	return (self->mode == PLM_BUFFER_MODE_FILE)
		? self->total_size
		: self->length;
}

size_t plm_buffer_get_remaining(plm_buffer_t *self) {
	return self->length - (self->bit_index >> 3);
}

size_t plm_buffer_write(plm_buffer_t *self, uint8_t *bytes, size_t length) {
	if (self->mode == PLM_BUFFER_MODE_FIXED_MEM) {
		return 0;
	}

	if (self->discard_read_bytes) {
		// This should be a ring buffer, but instead it just shifts all unread 
		// data to the beginning of the buffer and appends new data at the end. 
		// Seems to be good enough.

		plm_buffer_discard_read_bytes(self);
		if (self->mode == PLM_BUFFER_MODE_RING) {
			self->total_size = 0;
		}
	}

	// Do we have to resize to fit the new data?
	size_t bytes_available = self->capacity - self->length;
	if (bytes_available < length) {
		size_t new_size = self->capacity;
		do {
			new_size *= 2;
		} while (new_size - self->length < length);
		self->bytes = (uint8_t *)realloc(self->bytes, new_size);
		self->capacity = new_size;
	}

	memcpy(self->bytes + self->length, bytes, length);
	self->length += length;
	self->has_ended = FALSE;
	return length;
}

void plm_buffer_signal_end(plm_buffer_t *self) {
	self->total_size = self->length;
}

void plm_buffer_set_load_callback(plm_buffer_t *self, plm_buffer_load_callback fp, void *user) {
	self->load_callback = fp;
	self->load_callback_user_data = user;
}

void plm_buffer_rewind(plm_buffer_t *self) {
	plm_buffer_seek(self, 0);
}

void plm_buffer_seek(plm_buffer_t *self, size_t pos) {
	self->has_ended = FALSE;

	if (self->mode == PLM_BUFFER_MODE_FILE) {
		fseek(self->fh, pos, SEEK_SET);
		self->bit_index = 0;
		self->length = 0;
	}
	else if (self->mode == PLM_BUFFER_MODE_RING) {
		if (pos != 0) {
			// Seeking to non-0 is forbidden for dynamic-mem buffers
			return; 
		}
		self->bit_index = 0;
		self->length = 0;
		self->total_size = 0;
	}
	else if (pos < self->length) {
		self->bit_index = pos << 3;
	}
}

size_t plm_buffer_tell(plm_buffer_t *self) {
	return self->mode == PLM_BUFFER_MODE_FILE
		? ftell(self->fh) + (self->bit_index >> 3) - self->length
		: self->bit_index >> 3;
}

void plm_buffer_discard_read_bytes(plm_buffer_t *self) {
	size_t byte_pos = self->bit_index >> 3;
	if (byte_pos == self->length) {
		self->bit_index = 0;
		self->length = 0;
	}
	else if (byte_pos > 0) {
		memmove(self->bytes, self->bytes + byte_pos, self->length - byte_pos);
		self->bit_index -= byte_pos << 3;
		self->length -= byte_pos;
	}
}

void plm_buffer_load_file_callback(plm_buffer_t *self, void *user) {
	PLM_UNUSED(user);
	
	if (self->discard_read_bytes) {
		plm_buffer_discard_read_bytes(self);
	}

	size_t bytes_available = self->capacity - self->length;
	size_t bytes_read = fread(self->bytes + self->length, 1, bytes_available, self->fh);
	self->length += bytes_read;

	if (bytes_read == 0) {
		self->has_ended = TRUE;
	}
}

int plm_buffer_has_ended(plm_buffer_t *self) {
	return self->has_ended;
}

int plm_buffer_has(plm_buffer_t *self, size_t count) {
	if (((self->length << 3) - self->bit_index) >= count) {
		return TRUE;
	}

	if (self->load_callback) {
		self->load_callback(self, self->load_callback_user_data);
	}

	if (((self->length << 3) - self->bit_index) >= count) {
		return TRUE;
	}
	
	if (self->total_size != 0 && self->length == self->total_size) {
		self->has_ended = TRUE;
	}
	return FALSE;
}

int plm_buffer_read(plm_buffer_t *self, int count) {
	if (!plm_buffer_has(self, count)) {
		return 0;
	}

	int value = 0;
	while (count) {
		int current_byte = self->bytes[self->bit_index >> 3];

		int remaining = 8 - (self->bit_index & 7); // Remaining bits in byte
		int read = remaining < count ? remaining : count; // Bits in self run
		int shift = remaining - read;
		int mask = (0xff >> (8 - read));

		value = (value << read) | ((current_byte & (mask << shift)) >> shift);

		self->bit_index += read;
		count -= read;
	}

	return value;
}

void plm_buffer_align(plm_buffer_t *self) {
	self->bit_index = ((self->bit_index + 7) >> 3) << 3; // Align to next byte
}

void plm_buffer_skip(plm_buffer_t *self, size_t count) {
	if (plm_buffer_has(self, count)) {
		self->bit_index += count;
	}
}

int plm_buffer_skip_bytes(plm_buffer_t *self, uint8_t v) {
	plm_buffer_align(self);
	int skipped = 0;
	while (plm_buffer_has(self, 8) && self->bytes[self->bit_index >> 3] == v) {
		self->bit_index += 8;
		skipped++;
	}
	return skipped;
}

int plm_buffer_next_start_code(plm_buffer_t *self) {
	plm_buffer_align(self);

	while (plm_buffer_has(self, (5 << 3))) {
		size_t byte_index = (self->bit_index) >> 3;
		if (
			self->bytes[byte_index] == 0x00 &&
			self->bytes[byte_index + 1] == 0x00 &&
			self->bytes[byte_index + 2] == 0x01
		) {
			self->bit_index = (byte_index + 4) << 3;
			return self->bytes[byte_index + 3];
		}
		self->bit_index += 8;
	}
	return -1;
}

int plm_buffer_find_start_code(plm_buffer_t *self, int code) {
	int current = 0;
	while (TRUE) {
		current = plm_buffer_next_start_code(self);
		if (current == code || current == -1) {
			return current;
		}
	}
	return -1;
}

int plm_buffer_has_start_code(plm_buffer_t *self, int code) {
	size_t previous_bit_index = self->bit_index;
	int previous_discard_read_bytes = self->discard_read_bytes;
	
	self->discard_read_bytes = FALSE;
	int current = plm_buffer_find_start_code(self, code);

	self->bit_index = previous_bit_index;
	self->discard_read_bytes = previous_discard_read_bytes;
	return current;
}

int plm_buffer_no_start_code(plm_buffer_t *self) {
	if (!plm_buffer_has(self, (5 << 3))) {
		return FALSE;
	}

	size_t byte_index = ((self->bit_index + 7) >> 3);
	return !(
		self->bytes[byte_index] == 0x00 &&
		self->bytes[byte_index + 1] == 0x00 &&
		self->bytes[byte_index + 2] == 0x01
	);
}

int16_t plm_buffer_read_vlc(plm_buffer_t *self, const plm_vlc_t *table) {
	plm_vlc_t state = {0, 0};
	do {
		state = table[state.index + plm_buffer_read(self, 1)];
	} while (state.index > 0);
	return state.value;
}

uint16_t plm_buffer_read_vlc_uint(plm_buffer_t *self, const plm_vlc_uint_t *table) {
	return (uint16_t)plm_buffer_read_vlc(self, (const plm_vlc_t *)table);
}



// ----------------------------------------------------------------------------
// plm_demux implementation

static const int PLM_START_PACK = 0xBA;
static const int PLM_START_END = 0xB9;
static const int PLM_START_SYSTEM = 0xBB;

typedef struct plm_demux_t {
	plm_buffer_t *buffer;
	int destroy_buffer_when_done;
	double system_clock_ref;

	size_t last_file_size;
	double last_decoded_pts;
	double start_time;
	double duration;

	int start_code;
	int has_pack_header;
	int has_system_header;
	int has_headers;

	int num_audio_streams;
	int num_video_streams;
	plm_packet_t current_packet;
	plm_packet_t next_packet;
} plm_demux_t;


void plm_demux_buffer_seek(plm_demux_t *self, size_t pos);
double plm_demux_decode_time(plm_demux_t *self);
plm_packet_t *plm_demux_decode_packet(plm_demux_t *self, int type);
plm_packet_t *plm_demux_get_packet(plm_demux_t *self);

plm_demux_t *plm_demux_create(plm_buffer_t *buffer, int destroy_when_done) {
	plm_demux_t *self = (plm_demux_t *)malloc(sizeof(plm_demux_t));
	memset(self, 0, sizeof(plm_demux_t));

	self->buffer = buffer;
	self->destroy_buffer_when_done = destroy_when_done;

	self->start_time = PLM_PACKET_INVALID_TS;
	self->duration = PLM_PACKET_INVALID_TS;
	self->start_code = -1;

	plm_demux_has_headers(self);
	return self;
}

void plm_demux_destroy(plm_demux_t *self) {
	if (self->destroy_buffer_when_done) {
		plm_buffer_destroy(self->buffer);
	}
	free(self);
}

int plm_demux_has_headers(plm_demux_t *self) {
	if (self->has_headers) {
		return TRUE;
	}

	// Decode pack header
	if (!self->has_pack_header) {
		if (
			self->start_code != PLM_START_PACK &&
			plm_buffer_find_start_code(self->buffer, PLM_START_PACK) == -1
		) {
			return FALSE;
		}

		self->start_code = PLM_START_PACK;
		if (!plm_buffer_has(self->buffer, 64)) {
			return FALSE;
		}
		self->start_code = -1;

		if (plm_buffer_read(self->buffer, 4) != 0x02) {
			return FALSE;
		}

		self->system_clock_ref = plm_demux_decode_time(self);
		plm_buffer_skip(self->buffer, 1);
		plm_buffer_skip(self->buffer, 22); // mux_rate * 50
		plm_buffer_skip(self->buffer, 1);

		self->has_pack_header = TRUE;
	}

	// Decode system header
	if (!self->has_system_header) {
		if (
			self->start_code != PLM_START_SYSTEM &&
			plm_buffer_find_start_code(self->buffer, PLM_START_SYSTEM) == -1
		) {
			return FALSE;
		}

		self->start_code = PLM_START_SYSTEM;
		if (!plm_buffer_has(self->buffer, 56)) {
			return FALSE;
		}
		self->start_code = -1;

		plm_buffer_skip(self->buffer, 16); // header_length
		plm_buffer_skip(self->buffer, 24); // rate bound
		self->num_audio_streams = plm_buffer_read(self->buffer, 6);
		plm_buffer_skip(self->buffer, 5); // misc flags
		self->num_video_streams = plm_buffer_read(self->buffer, 5);

		self->has_system_header = TRUE;
	}

	self->has_headers = TRUE;
	return TRUE;
}

int plm_demux_get_num_video_streams(plm_demux_t *self) {
	return plm_demux_has_headers(self)
		? self->num_video_streams
		: 0;
}

int plm_demux_get_num_audio_streams(plm_demux_t *self) {
	return plm_demux_has_headers(self)
		? self->num_audio_streams
		: 0;
}

void plm_demux_rewind(plm_demux_t *self) {
	plm_buffer_rewind(self->buffer);
	self->current_packet.length = 0;
	self->next_packet.length = 0;
	self->start_code = -1;
}

int plm_demux_has_ended(plm_demux_t *self) {
	return plm_buffer_has_ended(self->buffer);
}

void plm_demux_buffer_seek(plm_demux_t *self, size_t pos) {
	plm_buffer_seek(self->buffer, pos);
	self->current_packet.length = 0;
	self->next_packet.length = 0;
	self->start_code = -1;
}

double plm_demux_get_start_time(plm_demux_t *self, int type) {
	if (self->start_time != PLM_PACKET_INVALID_TS) {
		return self->start_time;
	}

	int previous_pos = plm_buffer_tell(self->buffer);
	int previous_start_code = self->start_code;
	
	// Find first video PTS
	plm_demux_rewind(self);
	do {
		plm_packet_t *packet = plm_demux_decode(self);
		if (!packet) {
			break;
		}
		if (packet->type == type) {
			self->start_time = packet->pts;
		}
	} while (self->start_time == PLM_PACKET_INVALID_TS);

	plm_demux_buffer_seek(self, previous_pos);
	self->start_code = previous_start_code;
	return self->start_time;
}

double plm_demux_get_duration(plm_demux_t *self, int type) {
	size_t file_size = plm_buffer_get_size(self->buffer);

	if (
		self->duration != PLM_PACKET_INVALID_TS &&
		self->last_file_size == file_size
	) {
		return self->duration;
	}

	size_t previous_pos = plm_buffer_tell(self->buffer);
	int previous_start_code = self->start_code;
	
	// Find last video PTS. Start searching 64kb from the end and go further 
	// back if needed.
	long start_range = 64 * 1024;
	long max_range = 4096 * 1024;
	for (long range = start_range; range <= max_range; range *= 2) {
		long seek_pos = file_size - range;
		if (seek_pos < 0) {
			seek_pos = 0;
			range = max_range; // Make sure to bail after this round
		}
		plm_demux_buffer_seek(self, seek_pos);
		self->current_packet.length = 0;

		double last_pts = PLM_PACKET_INVALID_TS;
		plm_packet_t *packet = NULL;
		while ((packet = plm_demux_decode(self))) {
			if (packet->pts != PLM_PACKET_INVALID_TS && packet->type == type) {
				last_pts = packet->pts;
			}
		}
		if (last_pts != PLM_PACKET_INVALID_TS) {
			self->duration = last_pts - plm_demux_get_start_time(self, type);
			break;
		}
	}

	plm_demux_buffer_seek(self, previous_pos);
	self->start_code = previous_start_code;
	self->last_file_size = file_size;
	return self->duration;
}

plm_packet_t *plm_demux_seek(plm_demux_t *self, double seek_time, int type, int force_intra) {
	if (!plm_demux_has_headers(self)) {
		return NULL;
	}

	// Using the current time, current byte position and the average bytes per
	// second for this file, try to jump to a byte position that hopefully has
	// packets containing timestamps within one second before to the desired 
	// seek_time.

	// If we hit close to the seek_time scan through all packets to find the
	// last one (just before the seek_time) containing an intra frame.
	// Otherwise we should at least be closer than before. Calculate the bytes
	// per second for the jumped range and jump again.

	// The number of retries here is hard-limited to a generous amount. Usually
	// the correct range is found after 1--5 jumps, even for files with very 
	// variable bitrates. If significantly more jumps are needed, there's
	// probably something wrong with the file and we just avoid getting into an
	// infinite loop. 32 retries should be enough for anybody.

	double duration = plm_demux_get_duration(self, type);
	long file_size = plm_buffer_get_size(self->buffer);
	long byterate = file_size / duration;

	double cur_time = self->last_decoded_pts;
	double scan_span = 1;

	if (seek_time > duration) {
		seek_time = duration;
	}
	else if (seek_time < 0) {
		seek_time = 0;
	}
	seek_time += self->start_time;

	for (int retry = 0; retry < 32; retry++) {
		int found_packet_with_pts = FALSE;
		int found_packet_in_range = FALSE;
		long last_valid_packet_start = -1;
		double first_packet_time = PLM_PACKET_INVALID_TS;

		long cur_pos = plm_buffer_tell(self->buffer);

		// Estimate byte offset and jump to it.
		long offset = (seek_time - cur_time - scan_span) * byterate;
		long seek_pos = cur_pos + offset;
		if (seek_pos < 0) {
			seek_pos = 0;
		}
		else if (seek_pos > file_size - 256) {
			seek_pos = file_size - 256;
		}

		plm_demux_buffer_seek(self, seek_pos);

		// Scan through all packets up to the seek_time to find the last packet
		// containing an intra frame.
		while (plm_buffer_find_start_code(self->buffer, type) != -1) {
			long packet_start = plm_buffer_tell(self->buffer);
			plm_packet_t *packet = plm_demux_decode_packet(self, type);

			// Skip packet if it has no PTS
			if (!packet || packet->pts == PLM_PACKET_INVALID_TS) {
				continue;
			}

			// Bail scanning through packets if we hit one that is outside
			// seek_time - scan_span.
			// We also adjust the cur_time and byterate values here so the next 
			// iteration can be a bit more precise.
			if (packet->pts > seek_time || packet->pts < seek_time - scan_span) {
				found_packet_with_pts = TRUE;
				byterate = (seek_pos - cur_pos) / (packet->pts - cur_time);
				cur_time = packet->pts;
				break;
			}

			// If we are still here, it means this packet is in close range to
			// the seek_time. If this is the first packet for this jump position
			// record the PTS. If we later have to back off, when there was no
			// intra frame in this range, we can lower the seek_time to not scan
			// this range again.
			if (!found_packet_in_range) {
				found_packet_in_range = TRUE;
				first_packet_time = packet->pts;
			}

			// Check if this is an intra frame packet. If so, record the buffer
			// position of the start of this packet. We want to jump back to it 
			// later, when we know it's the last intra frame before desired
			// seek time.
			if (force_intra) {
				for (size_t i = 0; i < packet->length - 6; i++) {
					// Find the START_PICTURE code
					if (
						packet->data[i] == 0x00 &&
						packet->data[i + 1] == 0x00 &&
						packet->data[i + 2] == 0x01 &&
						packet->data[i + 3] == 0x00
					) {
						// Bits 11--13 in the picture header contain the frame 
						// type, where 1=Intra
						if ((packet->data[i + 5] & 0x38) == 8) {
							last_valid_packet_start = packet_start;
						}
						break;
					}
				}
			}

			// If we don't want intra frames, just use the last PTS found.
			else {
				last_valid_packet_start = packet_start;
			}
		}

		// If there was at least one intra frame in the range scanned above,
		// our search is over. Jump back to the packet and decode it again.
		if (last_valid_packet_start != -1) {
			plm_demux_buffer_seek(self, last_valid_packet_start);
			return plm_demux_decode_packet(self, type);
		}

		// If we hit the right range, but still found no intra frame, we have
		// to increases the scan_span. This is done exponentially to also handle
		// video files with very few intra frames.
		else if (found_packet_in_range) {
			scan_span *= 2;
			seek_time = first_packet_time;
		}

		// If we didn't find any packet with a PTS, it probably means we reached
		// the end of the file. Estimate byterate and cur_time accordingly.
		else if (!found_packet_with_pts) {
			byterate = (seek_pos - cur_pos) / (duration - cur_time);
			cur_time = duration;
		}
	}

	return NULL;
}

plm_packet_t *plm_demux_decode(plm_demux_t *self) {
	if (!plm_demux_has_headers(self)) {
		return NULL;
	}

	if (self->current_packet.length) {
		size_t bits_till_next_packet = self->current_packet.length << 3;
		if (!plm_buffer_has(self->buffer, bits_till_next_packet)) {
			return NULL;
		}
		plm_buffer_skip(self->buffer, bits_till_next_packet);
		self->current_packet.length = 0;
	}

	// Pending packet waiting for data?
	if (self->next_packet.length) {
		return plm_demux_get_packet(self);
	}

	// Pending packet waiting for header?
	if (self->start_code != -1) {
		return plm_demux_decode_packet(self, self->start_code);
	}

	do {
		self->start_code = plm_buffer_next_start_code(self->buffer);
		if (
			self->start_code == PLM_DEMUX_PACKET_VIDEO_1 || 
			self->start_code == PLM_DEMUX_PACKET_PRIVATE || (
				self->start_code >= PLM_DEMUX_PACKET_AUDIO_1 && 
				self->start_code <= PLM_DEMUX_PACKET_AUDIO_4
			)
		) {
			return plm_demux_decode_packet(self, self->start_code);
		}
	} while (self->start_code != -1);

	return NULL;
}

double plm_demux_decode_time(plm_demux_t *self) {
	int64_t clock = plm_buffer_read(self->buffer, 3) << 30;
	plm_buffer_skip(self->buffer, 1);
	clock |= plm_buffer_read(self->buffer, 15) << 15;
	plm_buffer_skip(self->buffer, 1);
	clock |= plm_buffer_read(self->buffer, 15);
	plm_buffer_skip(self->buffer, 1);
	return (double)clock / 90000.0;
}

plm_packet_t *plm_demux_decode_packet(plm_demux_t *self, int type) {
	if (!plm_buffer_has(self->buffer, 16 << 3)) {
		return NULL;
	}

	self->start_code = -1;

	self->next_packet.type = type;
	self->next_packet.length = plm_buffer_read(self->buffer, 16);
	self->next_packet.length -= plm_buffer_skip_bytes(self->buffer, 0xff); // stuffing

	// skip P-STD
	if (plm_buffer_read(self->buffer, 2) == 0x01) {
		plm_buffer_skip(self->buffer, 16);
		self->next_packet.length -= 2;
	}

	int pts_dts_marker = plm_buffer_read(self->buffer, 2);
	if (pts_dts_marker == 0x03) {
		self->next_packet.pts = plm_demux_decode_time(self);
		self->last_decoded_pts = self->next_packet.pts;
		plm_buffer_skip(self->buffer, 40); // skip dts
		self->next_packet.length -= 10;
	}
	else if (pts_dts_marker == 0x02) {
		self->next_packet.pts = plm_demux_decode_time(self);
		self->last_decoded_pts = self->next_packet.pts;
		self->next_packet.length -= 5;
	}
	else if (pts_dts_marker == 0x00) {
		self->next_packet.pts = PLM_PACKET_INVALID_TS;
		plm_buffer_skip(self->buffer, 4);
		self->next_packet.length -= 1;
	}
	else {
		return NULL; // invalid
	}
	
	return plm_demux_get_packet(self);
}

plm_packet_t *plm_demux_get_packet(plm_demux_t *self) {
	if (!plm_buffer_has(self->buffer, self->next_packet.length << 3)) {
		return NULL;
	}

	self->current_packet.data = self->buffer->bytes + (self->buffer->bit_index >> 3);
	self->current_packet.length = self->next_packet.length;
	self->current_packet.type = self->next_packet.type;
	self->current_packet.pts = self->next_packet.pts;

	self->next_packet.length = 0;
	return &self->current_packet;
}



// -----------------------------------------------------------------------------
// plm_video implementation

// Inspired by Java MPEG-1 Video Decoder and Player by Zoltan Korandi 
// https://sourceforge.net/projects/javampeg1video/

static const int PLM_VIDEO_PICTURE_TYPE_INTRA = 1;
static const int PLM_VIDEO_PICTURE_TYPE_PREDICTIVE = 2;
static const int PLM_VIDEO_PICTURE_TYPE_B = 3;

static const int PLM_START_SEQUENCE = 0xB3;
static const int PLM_START_SLICE_FIRST = 0x01;
static const int PLM_START_SLICE_LAST = 0xAF;
static const int PLM_START_PICTURE = 0x00;
static const int PLM_START_EXTENSION = 0xB5;
static const int PLM_START_USER_DATA = 0xB2;

#define PLM_START_IS_SLICE(c) \
	(c >= PLM_START_SLICE_FIRST && c <= PLM_START_SLICE_LAST)

static const double PLM_VIDEO_PICTURE_RATE[] = {
	0.000, 23.976, 24.000, 25.000, 29.970, 30.000, 50.000, 59.940,
	60.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000
};

static const uint8_t PLM_VIDEO_ZIG_ZAG[] = {
	 0,  1,  8, 16,  9,  2,  3, 10,
	17, 24, 32, 25, 18, 11,  4,  5,
	12, 19, 26, 33, 40, 48, 41, 34,
	27, 20, 13,  6,  7, 14, 21, 28,
	35, 42, 49, 56, 57, 50, 43, 36,
	29, 22, 15, 23, 30, 37, 44, 51,
	58, 59, 52, 45, 38, 31, 39, 46,
	53, 60, 61, 54, 47, 55, 62, 63
};

static const uint8_t PLM_VIDEO_INTRA_QUANT_MATRIX[] = {
	 8, 16, 19, 22, 26, 27, 29, 34,
	16, 16, 22, 24, 27, 29, 34, 37,
	19, 22, 26, 27, 29, 34, 34, 38,
	22, 22, 26, 27, 29, 34, 37, 40,
	22, 26, 27, 29, 32, 35, 40, 48,
	26, 27, 29, 32, 35, 40, 48, 58,
	26, 27, 29, 34, 38, 46, 56, 69,
	27, 29, 35, 38, 46, 56, 69, 83
};

static const uint8_t PLM_VIDEO_NON_INTRA_QUANT_MATRIX[] = {
	16, 16, 16, 16, 16, 16, 16, 16,
	16, 16, 16, 16, 16, 16, 16, 16,
	16, 16, 16, 16, 16, 16, 16, 16,
	16, 16, 16, 16, 16, 16, 16, 16,
	16, 16, 16, 16, 16, 16, 16, 16,
	16, 16, 16, 16, 16, 16, 16, 16,
	16, 16, 16, 16, 16, 16, 16, 16,
	16, 16, 16, 16, 16, 16, 16, 16
};

static const uint8_t PLM_VIDEO_PREMULTIPLIER_MATRIX[] = {
	32, 44, 42, 38, 32, 25, 17,  9,
	44, 62, 58, 52, 44, 35, 24, 12,
	42, 58, 55, 49, 42, 33, 23, 12,
	38, 52, 49, 44, 38, 30, 20, 10,
	32, 44, 42, 38, 32, 25, 17,  9,
	25, 35, 33, 30, 25, 20, 14,  7,
	17, 24, 23, 20, 17, 14,  9,  5,
	 9, 12, 12, 10,  9,  7,  5,  2
};

static const plm_vlc_t PLM_VIDEO_MACROBLOCK_ADDRESS_INCREMENT[] = {
	{  1 << 1,    0}, {       0,    1},  //   0: x
	{  2 << 1,    0}, {  3 << 1,    0},  //   1: 0x
	{  4 << 1,    0}, {  5 << 1,    0},  //   2: 00x
	{       0,    3}, {       0,    2},  //   3: 01x
	{  6 << 1,    0}, {  7 << 1,    0},  //   4: 000x
	{       0,    5}, {       0,    4},  //   5: 001x
	{  8 << 1,    0}, {  9 << 1,    0},  //   6: 0000x
	{       0,    7}, {       0,    6},  //   7: 0001x
	{ 10 << 1,    0}, { 11 << 1,    0},  //   8: 0000 0x
	{ 12 << 1,    0}, { 13 << 1,    0},  //   9: 0000 1x
	{ 14 << 1,    0}, { 15 << 1,    0},  //  10: 0000 00x
	{ 16 << 1,    0}, { 17 << 1,    0},  //  11: 0000 01x
	{ 18 << 1,    0}, { 19 << 1,    0},  //  12: 0000 10x
	{       0,    9}, {       0,    8},  //  13: 0000 11x
	{      -1,    0}, { 20 << 1,    0},  //  14: 0000 000x
	{      -1,    0}, { 21 << 1,    0},  //  15: 0000 001x
	{ 22 << 1,    0}, { 23 << 1,    0},  //  16: 0000 010x
	{       0,   15}, {       0,   14},  //  17: 0000 011x
	{       0,   13}, {       0,   12},  //  18: 0000 100x
	{       0,   11}, {       0,   10},  //  19: 0000 101x
	{ 24 << 1,    0}, { 25 << 1,    0},  //  20: 0000 0001x
	{ 26 << 1,    0}, { 27 << 1,    0},  //  21: 0000 0011x
	{ 28 << 1,    0}, { 29 << 1,    0},  //  22: 0000 0100x
	{ 30 << 1,    0}, { 31 << 1,    0},  //  23: 0000 0101x
	{ 32 << 1,    0}, {      -1,    0},  //  24: 0000 0001 0x
	{      -1,    0}, { 33 << 1,    0},  //  25: 0000 0001 1x
	{ 34 << 1,    0}, { 35 << 1,    0},  //  26: 0000 0011 0x
	{ 36 << 1,    0}, { 37 << 1,    0},  //  27: 0000 0011 1x
	{ 38 << 1,    0}, { 39 << 1,    0},  //  28: 0000 0100 0x
	{       0,   21}, {       0,   20},  //  29: 0000 0100 1x
	{       0,   19}, {       0,   18},  //  30: 0000 0101 0x
	{       0,   17}, {       0,   16},  //  31: 0000 0101 1x
	{       0,   35}, {      -1,    0},  //  32: 0000 0001 00x
	{      -1,    0}, {       0,   34},  //  33: 0000 0001 11x
	{       0,   33}, {       0,   32},  //  34: 0000 0011 00x
	{       0,   31}, {       0,   30},  //  35: 0000 0011 01x
	{       0,   29}, {       0,   28},  //  36: 0000 0011 10x
	{       0,   27}, {       0,   26},  //  37: 0000 0011 11x
	{       0,   25}, {       0,   24},  //  38: 0000 0100 00x
	{       0,   23}, {       0,   22},  //  39: 0000 0100 01x
};

static const plm_vlc_t PLM_VIDEO_MACROBLOCK_TYPE_INTRA[] = {
	{  1 << 1,    0}, {       0,  0x01},  //   0: x
	{      -1,    0}, {       0,  0x11},  //   1: 0x
};

static const plm_vlc_t PLM_VIDEO_MACROBLOCK_TYPE_PREDICTIVE[] = {
	{  1 << 1,    0}, {       0, 0x0a},  //   0: x
	{  2 << 1,    0}, {       0, 0x02},  //   1: 0x
	{  3 << 1,    0}, {       0, 0x08},  //   2: 00x
	{  4 << 1,    0}, {  5 << 1,    0},  //   3: 000x
	{  6 << 1,    0}, {       0, 0x12},  //   4: 0000x
	{       0, 0x1a}, {       0, 0x01},  //   5: 0001x
	{      -1,    0}, {       0, 0x11},  //   6: 0000 0x
};

static const plm_vlc_t PLM_VIDEO_MACROBLOCK_TYPE_B[] = {
	{  1 << 1,    0}, {  2 << 1,    0},  //   0: x
	{  3 << 1,    0}, {  4 << 1,    0},  //   1: 0x
	{       0, 0x0c}, {       0, 0x0e},  //   2: 1x
	{  5 << 1,    0}, {  6 << 1,    0},  //   3: 00x
	{       0, 0x04}, {       0, 0x06},  //   4: 01x
	{  7 << 1,    0}, {  8 << 1,    0},  //   5: 000x
	{       0, 0x08}, {       0, 0x0a},  //   6: 001x
	{  9 << 1,    0}, { 10 << 1,    0},  //   7: 0000x
	{       0, 0x1e}, {       0, 0x01},  //   8: 0001x
	{      -1,    0}, {       0, 0x11},  //   9: 0000 0x
	{       0, 0x16}, {       0, 0x1a},  //  10: 0000 1x
};

static const plm_vlc_t *PLM_VIDEO_MACROBLOCK_TYPE[] = {
	NULL,
	PLM_VIDEO_MACROBLOCK_TYPE_INTRA,
	PLM_VIDEO_MACROBLOCK_TYPE_PREDICTIVE,
	PLM_VIDEO_MACROBLOCK_TYPE_B
};

static const plm_vlc_t PLM_VIDEO_CODE_BLOCK_PATTERN[] = {
	{  1 << 1,    0}, {  2 << 1,    0},  //   0: x
	{  3 << 1,    0}, {  4 << 1,    0},  //   1: 0x
	{  5 << 1,    0}, {  6 << 1,    0},  //   2: 1x
	{  7 << 1,    0}, {  8 << 1,    0},  //   3: 00x
	{  9 << 1,    0}, { 10 << 1,    0},  //   4: 01x
	{ 11 << 1,    0}, { 12 << 1,    0},  //   5: 10x
	{ 13 << 1,    0}, {       0,   60},  //   6: 11x
	{ 14 << 1,    0}, { 15 << 1,    0},  //   7: 000x
	{ 16 << 1,    0}, { 17 << 1,    0},  //   8: 001x
	{ 18 << 1,    0}, { 19 << 1,    0},  //   9: 010x
	{ 20 << 1,    0}, { 21 << 1,    0},  //  10: 011x
	{ 22 << 1,    0}, { 23 << 1,    0},  //  11: 100x
	{       0,   32}, {       0,   16},  //  12: 101x
	{       0,    8}, {       0,    4},  //  13: 110x
	{ 24 << 1,    0}, { 25 << 1,    0},  //  14: 0000x
	{ 26 << 1,    0}, { 27 << 1,    0},  //  15: 0001x
	{ 28 << 1,    0}, { 29 << 1,    0},  //  16: 0010x
	{ 30 << 1,    0}, { 31 << 1,    0},  //  17: 0011x
	{       0,   62}, {       0,    2},  //  18: 0100x
	{       0,   61}, {       0,    1},  //  19: 0101x
	{       0,   56}, {       0,   52},  //  20: 0110x
	{       0,   44}, {       0,   28},  //  21: 0111x
	{       0,   40}, {       0,   20},  //  22: 1000x
	{       0,   48}, {       0,   12},  //  23: 1001x
	{ 32 << 1,    0}, { 33 << 1,    0},  //  24: 0000 0x
	{ 34 << 1,    0}, { 35 << 1,    0},  //  25: 0000 1x
	{ 36 << 1,    0}, { 37 << 1,    0},  //  26: 0001 0x
	{ 38 << 1,    0}, { 39 << 1,    0},  //  27: 0001 1x
	{ 40 << 1,    0}, { 41 << 1,    0},  //  28: 0010 0x
	{ 42 << 1,    0}, { 43 << 1,    0},  //  29: 0010 1x
	{       0,   63}, {       0,    3},  //  30: 0011 0x
	{       0,   36}, {       0,   24},  //  31: 0011 1x
	{ 44 << 1,    0}, { 45 << 1,    0},  //  32: 0000 00x
	{ 46 << 1,    0}, { 47 << 1,    0},  //  33: 0000 01x
	{ 48 << 1,    0}, { 49 << 1,    0},  //  34: 0000 10x
	{ 50 << 1,    0}, { 51 << 1,    0},  //  35: 0000 11x
	{ 52 << 1,    0}, { 53 << 1,    0},  //  36: 0001 00x
	{ 54 << 1,    0}, { 55 << 1,    0},  //  37: 0001 01x
	{ 56 << 1,    0}, { 57 << 1,    0},  //  38: 0001 10x
	{ 58 << 1,    0}, { 59 << 1,    0},  //  39: 0001 11x
	{       0,   34}, {       0,   18},  //  40: 0010 00x
	{       0,   10}, {       0,    6},  //  41: 0010 01x
	{       0,   33}, {       0,   17},  //  42: 0010 10x
	{       0,    9}, {       0,    5},  //  43: 0010 11x
	{      -1,    0}, { 60 << 1,    0},  //  44: 0000 000x
	{ 61 << 1,    0}, { 62 << 1,    0},  //  45: 0000 001x
	{       0,   58}, {       0,   54},  //  46: 0000 010x
	{       0,   46}, {       0,   30},  //  47: 0000 011x
	{       0,   57}, {       0,   53},  //  48: 0000 100x
	{       0,   45}, {       0,   29},  //  49: 0000 101x
	{       0,   38}, {       0,   26},  //  50: 0000 110x
	{       0,   37}, {       0,   25},  //  51: 0000 111x
	{       0,   43}, {       0,   23},  //  52: 0001 000x
	{       0,   51}, {       0,   15},  //  53: 0001 001x
	{       0,   42}, {       0,   22},  //  54: 0001 010x
	{       0,   50}, {       0,   14},  //  55: 0001 011x
	{       0,   41}, {       0,   21},  //  56: 0001 100x
	{       0,   49}, {       0,   13},  //  57: 0001 101x
	{       0,   35}, {       0,   19},  //  58: 0001 110x
	{       0,   11}, {       0,    7},  //  59: 0001 111x
	{       0,   39}, {       0,   27},  //  60: 0000 0001x
	{       0,   59}, {       0,   55},  //  61: 0000 0010x
	{       0,   47}, {       0,   31},  //  62: 0000 0011x
};

static const plm_vlc_t PLM_VIDEO_MOTION[] = {
	{  1 << 1,    0}, {       0,    0},  //   0: x
	{  2 << 1,    0}, {  3 << 1,    0},  //   1: 0x
	{  4 << 1,    0}, {  5 << 1,    0},  //   2: 00x
	{       0,    1}, {       0,   -1},  //   3: 01x
	{  6 << 1,    0}, {  7 << 1,    0},  //   4: 000x
	{       0,    2}, {       0,   -2},  //   5: 001x
	{  8 << 1,    0}, {  9 << 1,    0},  //   6: 0000x
	{       0,    3}, {       0,   -3},  //   7: 0001x
	{ 10 << 1,    0}, { 11 << 1,    0},  //   8: 0000 0x
	{ 12 << 1,    0}, { 13 << 1,    0},  //   9: 0000 1x
	{      -1,    0}, { 14 << 1,    0},  //  10: 0000 00x
	{ 15 << 1,    0}, { 16 << 1,    0},  //  11: 0000 01x
	{ 17 << 1,    0}, { 18 << 1,    0},  //  12: 0000 10x
	{       0,    4}, {       0,   -4},  //  13: 0000 11x
	{      -1,    0}, { 19 << 1,    0},  //  14: 0000 001x
	{ 20 << 1,    0}, { 21 << 1,    0},  //  15: 0000 010x
	{       0,    7}, {       0,   -7},  //  16: 0000 011x
	{       0,    6}, {       0,   -6},  //  17: 0000 100x
	{       0,    5}, {       0,   -5},  //  18: 0000 101x
	{ 22 << 1,    0}, { 23 << 1,    0},  //  19: 0000 0011x
	{ 24 << 1,    0}, { 25 << 1,    0},  //  20: 0000 0100x
	{ 26 << 1,    0}, { 27 << 1,    0},  //  21: 0000 0101x
	{ 28 << 1,    0}, { 29 << 1,    0},  //  22: 0000 0011 0x
	{ 30 << 1,    0}, { 31 << 1,    0},  //  23: 0000 0011 1x
	{ 32 << 1,    0}, { 33 << 1,    0},  //  24: 0000 0100 0x
	{       0,   10}, {       0,  -10},  //  25: 0000 0100 1x
	{       0,    9}, {       0,   -9},  //  26: 0000 0101 0x
	{       0,    8}, {       0,   -8},  //  27: 0000 0101 1x
	{       0,   16}, {       0,  -16},  //  28: 0000 0011 00x
	{       0,   15}, {       0,  -15},  //  29: 0000 0011 01x
	{       0,   14}, {       0,  -14},  //  30: 0000 0011 10x
	{       0,   13}, {       0,  -13},  //  31: 0000 0011 11x
	{       0,   12}, {       0,  -12},  //  32: 0000 0100 00x
	{       0,   11}, {       0,  -11},  //  33: 0000 0100 01x
};

static const plm_vlc_t PLM_VIDEO_DCT_SIZE_LUMINANCE[] = {
	{  1 << 1,    0}, {  2 << 1,    0},  //   0: x
	{       0,    1}, {       0,    2},  //   1: 0x
	{  3 << 1,    0}, {  4 << 1,    0},  //   2: 1x
	{       0,    0}, {       0,    3},  //   3: 10x
	{       0,    4}, {  5 << 1,    0},  //   4: 11x
	{       0,    5}, {  6 << 1,    0},  //   5: 111x
	{       0,    6}, {  7 << 1,    0},  //   6: 1111x
	{       0,    7}, {  8 << 1,    0},  //   7: 1111 1x
	{       0,    8}, {      -1,    0},  //   8: 1111 11x
};

static const plm_vlc_t PLM_VIDEO_DCT_SIZE_CHROMINANCE[] = {
	{  1 << 1,    0}, {  2 << 1,    0},  //   0: x
	{       0,    0}, {       0,    1},  //   1: 0x
	{       0,    2}, {  3 << 1,    0},  //   2: 1x
	{       0,    3}, {  4 << 1,    0},  //   3: 11x
	{       0,    4}, {  5 << 1,    0},  //   4: 111x
	{       0,    5}, {  6 << 1,    0},  //   5: 1111x
	{       0,    6}, {  7 << 1,    0},  //   6: 1111 1x
	{       0,    7}, {  8 << 1,    0},  //   7: 1111 11x
	{       0,    8}, {      -1,    0},  //   8: 1111 111x
};

static const plm_vlc_t *PLM_VIDEO_DCT_SIZE[] = {
	PLM_VIDEO_DCT_SIZE_LUMINANCE,
	PLM_VIDEO_DCT_SIZE_CHROMINANCE,
	PLM_VIDEO_DCT_SIZE_CHROMINANCE
};


//  dct_coeff bitmap:
//    0xff00  run
//    0x00ff  level

//  Decoded values are unsigned. Sign bit follows in the stream.

static const plm_vlc_uint_t PLM_VIDEO_DCT_COEFF[] = {
	{  1 << 1,        0}, {       0,   0x0001},  //   0: x
	{  2 << 1,        0}, {  3 << 1,        0},  //   1: 0x
	{  4 << 1,        0}, {  5 << 1,        0},  //   2: 00x
	{  6 << 1,        0}, {       0,   0x0101},  //   3: 01x
	{  7 << 1,        0}, {  8 << 1,        0},  //   4: 000x
	{  9 << 1,        0}, { 10 << 1,        0},  //   5: 001x
	{       0,   0x0002}, {       0,   0x0201},  //   6: 010x
	{ 11 << 1,        0}, { 12 << 1,        0},  //   7: 0000x
	{ 13 << 1,        0}, { 14 << 1,        0},  //   8: 0001x
	{ 15 << 1,        0}, {       0,   0x0003},  //   9: 0010x
	{       0,   0x0401}, {       0,   0x0301},  //  10: 0011x
	{ 16 << 1,        0}, {       0,   0xffff},  //  11: 0000 0x
	{ 17 << 1,        0}, { 18 << 1,        0},  //  12: 0000 1x
	{       0,   0x0701}, {       0,   0x0601},  //  13: 0001 0x
	{       0,   0x0102}, {       0,   0x0501},  //  14: 0001 1x
	{ 19 << 1,        0}, { 20 << 1,        0},  //  15: 0010 0x
	{ 21 << 1,        0}, { 22 << 1,        0},  //  16: 0000 00x
	{       0,   0x0202}, {       0,   0x0901},  //  17: 0000 10x
	{       0,   0x0004}, {       0,   0x0801},  //  18: 0000 11x
	{ 23 << 1,        0}, { 24 << 1,        0},  //  19: 0010 00x
	{ 25 << 1,        0}, { 26 << 1,        0},  //  20: 0010 01x
	{ 27 << 1,        0}, { 28 << 1,        0},  //  21: 0000 000x
	{ 29 << 1,        0}, { 30 << 1,        0},  //  22: 0000 001x
	{       0,   0x0d01}, {       0,   0x0006},  //  23: 0010 000x
	{       0,   0x0c01}, {       0,   0x0b01},  //  24: 0010 001x
	{       0,   0x0302}, {       0,   0x0103},  //  25: 0010 010x
	{       0,   0x0005}, {       0,   0x0a01},  //  26: 0010 011x
	{ 31 << 1,        0}, { 32 << 1,        0},  //  27: 0000 0000x
	{ 33 << 1,        0}, { 34 << 1,        0},  //  28: 0000 0001x
	{ 35 << 1,        0}, { 36 << 1,        0},  //  29: 0000 0010x
	{ 37 << 1,        0}, { 38 << 1,        0},  //  30: 0000 0011x
	{ 39 << 1,        0}, { 40 << 1,        0},  //  31: 0000 0000 0x
	{ 41 << 1,        0}, { 42 << 1,        0},  //  32: 0000 0000 1x
	{ 43 << 1,        0}, { 44 << 1,        0},  //  33: 0000 0001 0x
	{ 45 << 1,        0}, { 46 << 1,        0},  //  34: 0000 0001 1x
	{       0,   0x1001}, {       0,   0x0502},  //  35: 0000 0010 0x
	{       0,   0x0007}, {       0,   0x0203},  //  36: 0000 0010 1x
	{       0,   0x0104}, {       0,   0x0f01},  //  37: 0000 0011 0x
	{       0,   0x0e01}, {       0,   0x0402},  //  38: 0000 0011 1x
	{ 47 << 1,        0}, { 48 << 1,        0},  //  39: 0000 0000 00x
	{ 49 << 1,        0}, { 50 << 1,        0},  //  40: 0000 0000 01x
	{ 51 << 1,        0}, { 52 << 1,        0},  //  41: 0000 0000 10x
	{ 53 << 1,        0}, { 54 << 1,        0},  //  42: 0000 0000 11x
	{ 55 << 1,        0}, { 56 << 1,        0},  //  43: 0000 0001 00x
	{ 57 << 1,        0}, { 58 << 1,        0},  //  44: 0000 0001 01x
	{ 59 << 1,        0}, { 60 << 1,        0},  //  45: 0000 0001 10x
	{ 61 << 1,        0}, { 62 << 1,        0},  //  46: 0000 0001 11x
	{      -1,        0}, { 63 << 1,        0},  //  47: 0000 0000 000x
	{ 64 << 1,        0}, { 65 << 1,        0},  //  48: 0000 0000 001x
	{ 66 << 1,        0}, { 67 << 1,        0},  //  49: 0000 0000 010x
	{ 68 << 1,        0}, { 69 << 1,        0},  //  50: 0000 0000 011x
	{ 70 << 1,        0}, { 71 << 1,        0},  //  51: 0000 0000 100x
	{ 72 << 1,        0}, { 73 << 1,        0},  //  52: 0000 0000 101x
	{ 74 << 1,        0}, { 75 << 1,        0},  //  53: 0000 0000 110x
	{ 76 << 1,        0}, { 77 << 1,        0},  //  54: 0000 0000 111x
	{       0,   0x000b}, {       0,   0x0802},  //  55: 0000 0001 000x
	{       0,   0x0403}, {       0,   0x000a},  //  56: 0000 0001 001x
	{       0,   0x0204}, {       0,   0x0702},  //  57: 0000 0001 010x
	{       0,   0x1501}, {       0,   0x1401},  //  58: 0000 0001 011x
	{       0,   0x0009}, {       0,   0x1301},  //  59: 0000 0001 100x
	{       0,   0x1201}, {       0,   0x0105},  //  60: 0000 0001 101x
	{       0,   0x0303}, {       0,   0x0008},  //  61: 0000 0001 110x
	{       0,   0x0602}, {       0,   0x1101},  //  62: 0000 0001 111x
	{ 78 << 1,        0}, { 79 << 1,        0},  //  63: 0000 0000 0001x
	{ 80 << 1,        0}, { 81 << 1,        0},  //  64: 0000 0000 0010x
	{ 82 << 1,        0}, { 83 << 1,        0},  //  65: 0000 0000 0011x
	{ 84 << 1,        0}, { 85 << 1,        0},  //  66: 0000 0000 0100x
	{ 86 << 1,        0}, { 87 << 1,        0},  //  67: 0000 0000 0101x
	{ 88 << 1,        0}, { 89 << 1,        0},  //  68: 0000 0000 0110x
	{ 90 << 1,        0}, { 91 << 1,        0},  //  69: 0000 0000 0111x
	{       0,   0x0a02}, {       0,   0x0902},  //  70: 0000 0000 1000x
	{       0,   0x0503}, {       0,   0x0304},  //  71: 0000 0000 1001x
	{       0,   0x0205}, {       0,   0x0107},  //  72: 0000 0000 1010x
	{       0,   0x0106}, {       0,   0x000f},  //  73: 0000 0000 1011x
	{       0,   0x000e}, {       0,   0x000d},  //  74: 0000 0000 1100x
	{       0,   0x000c}, {       0,   0x1a01},  //  75: 0000 0000 1101x
	{       0,   0x1901}, {       0,   0x1801},  //  76: 0000 0000 1110x
	{       0,   0x1701}, {       0,   0x1601},  //  77: 0000 0000 1111x
	{ 92 << 1,        0}, { 93 << 1,        0},  //  78: 0000 0000 0001 0x
	{ 94 << 1,        0}, { 95 << 1,        0},  //  79: 0000 0000 0001 1x
	{ 96 << 1,        0}, { 97 << 1,        0},  //  80: 0000 0000 0010 0x
	{ 98 << 1,        0}, { 99 << 1,        0},  //  81: 0000 0000 0010 1x
	{100 << 1,        0}, {101 << 1,        0},  //  82: 0000 0000 0011 0x
	{102 << 1,        0}, {103 << 1,        0},  //  83: 0000 0000 0011 1x
	{       0,   0x001f}, {       0,   0x001e},  //  84: 0000 0000 0100 0x
	{       0,   0x001d}, {       0,   0x001c},  //  85: 0000 0000 0100 1x
	{       0,   0x001b}, {       0,   0x001a},  //  86: 0000 0000 0101 0x
	{       0,   0x0019}, {       0,   0x0018},  //  87: 0000 0000 0101 1x
	{       0,   0x0017}, {       0,   0x0016},  //  88: 0000 0000 0110 0x
	{       0,   0x0015}, {       0,   0x0014},  //  89: 0000 0000 0110 1x
	{       0,   0x0013}, {       0,   0x0012},  //  90: 0000 0000 0111 0x
	{       0,   0x0011}, {       0,   0x0010},  //  91: 0000 0000 0111 1x
	{104 << 1,        0}, {105 << 1,        0},  //  92: 0000 0000 0001 00x
	{106 << 1,        0}, {107 << 1,        0},  //  93: 0000 0000 0001 01x
	{108 << 1,        0}, {109 << 1,        0},  //  94: 0000 0000 0001 10x
	{110 << 1,        0}, {111 << 1,        0},  //  95: 0000 0000 0001 11x
	{       0,   0x0028}, {       0,   0x0027},  //  96: 0000 0000 0010 00x
	{       0,   0x0026}, {       0,   0x0025},  //  97: 0000 0000 0010 01x
	{       0,   0x0024}, {       0,   0x0023},  //  98: 0000 0000 0010 10x
	{       0,   0x0022}, {       0,   0x0021},  //  99: 0000 0000 0010 11x
	{       0,   0x0020}, {       0,   0x010e},  // 100: 0000 0000 0011 00x
	{       0,   0x010d}, {       0,   0x010c},  // 101: 0000 0000 0011 01x
	{       0,   0x010b}, {       0,   0x010a},  // 102: 0000 0000 0011 10x
	{       0,   0x0109}, {       0,   0x0108},  // 103: 0000 0000 0011 11x
	{       0,   0x0112}, {       0,   0x0111},  // 104: 0000 0000 0001 000x
	{       0,   0x0110}, {       0,   0x010f},  // 105: 0000 0000 0001 001x
	{       0,   0x0603}, {       0,   0x1002},  // 106: 0000 0000 0001 010x
	{       0,   0x0f02}, {       0,   0x0e02},  // 107: 0000 0000 0001 011x
	{       0,   0x0d02}, {       0,   0x0c02},  // 108: 0000 0000 0001 100x
	{       0,   0x0b02}, {       0,   0x1f01},  // 109: 0000 0000 0001 101x
	{       0,   0x1e01}, {       0,   0x1d01},  // 110: 0000 0000 0001 110x
	{       0,   0x1c01}, {       0,   0x1b01},  // 111: 0000 0000 0001 111x
};

typedef struct {
	int full_px;
	int is_set;
	int r_size;
	int h;
	int v;
} plm_video_motion_t;

typedef struct plm_video_t {
	double framerate;
	double time;
	int frames_decoded;
	int width;
	int height;
	int mb_width;
	int mb_height;
	int mb_size;

	int luma_width;
	int luma_height;

	int chroma_width;
	int chroma_height;

	int start_code;
	int picture_type;

	plm_video_motion_t motion_forward;
	plm_video_motion_t motion_backward;

	int has_sequence_header;

	int quantizer_scale;
	int slice_begin;
	int macroblock_address;

	int mb_row;
	int mb_col;

	int macroblock_type;
	int macroblock_intra;

	int dc_predictor[3];

	plm_buffer_t *buffer;
	int destroy_buffer_when_done;

	plm_frame_t frame_current;
	plm_frame_t frame_forward;
	plm_frame_t frame_backward;

	uint8_t *frames_data;

	int block_data[64];
	uint8_t intra_quant_matrix[64];
	uint8_t non_intra_quant_matrix[64];

	int has_reference_frame;
	int assume_no_b_frames;
} plm_video_t;

static inline uint8_t plm_clamp(int n) {
	return n > 255
		? 255
		: (n < 0 ? 0 : n);
}

int plm_video_decode_sequence_header(plm_video_t *self);
void plm_video_init_frame(plm_video_t *self, plm_frame_t *frame, uint8_t *base);
void plm_video_decode_picture(plm_video_t *self);
void plm_video_decode_slice(plm_video_t *self, int slice);
void plm_video_decode_macroblock(plm_video_t *self);
void plm_video_decode_motion_vectors(plm_video_t *self);
int plm_video_decode_motion_vector(plm_video_t *self, int r_size, int motion);
void plm_video_predict_macroblock(plm_video_t *self);
void plm_video_copy_macroblock(plm_video_t *self, int motion_h, int motion_v, plm_frame_t *d);
void plm_video_interpolate_macroblock(plm_video_t *self, int motion_h, int motion_v, plm_frame_t *d);
void plm_video_process_macroblock(plm_video_t *self, uint8_t *d, uint8_t *s, int mh, int mb, int bs, int interp);
void plm_video_decode_block(plm_video_t *self, int block);
void plm_video_idct(int *block);

plm_video_t * plm_video_create_with_buffer(plm_buffer_t *buffer, int destroy_when_done) {
	plm_video_t *self = (plm_video_t *)malloc(sizeof(plm_video_t));
	memset(self, 0, sizeof(plm_video_t));
	
	self->buffer = buffer;
	self->destroy_buffer_when_done = destroy_when_done;

	// Attempt to decode the sequence header
	self->start_code = plm_buffer_find_start_code(self->buffer, PLM_START_SEQUENCE);
	if (self->start_code != -1) {
		plm_video_decode_sequence_header(self);
	}
	return self;
}

void plm_video_destroy(plm_video_t *self) {
	if (self->destroy_buffer_when_done) {
		plm_buffer_destroy(self->buffer);
	}

	if (self->has_sequence_header) {
		free(self->frames_data);
	}

	free(self);
}

double plm_video_get_framerate(plm_video_t *self) {
	return plm_video_has_header(self)
		? self->framerate
		: 0;
}

int plm_video_get_width(plm_video_t *self) {
	return plm_video_has_header(self)
		? self->width
		: 0;
}

int plm_video_get_height(plm_video_t *self) {
	return plm_video_has_header(self)
		? self->height
		: 0;
}

void plm_video_set_no_delay(plm_video_t *self, int no_delay) {
	self->assume_no_b_frames = no_delay;
}

double plm_video_get_time(plm_video_t *self) {
	return self->time;
}

void plm_video_set_time(plm_video_t *self, double time) {
	self->frames_decoded = self->framerate * time;
	self->time = time;
}

void plm_video_rewind(plm_video_t *self) {
	plm_buffer_rewind(self->buffer);
	self->time = 0;
	self->frames_decoded = 0;
	self->has_reference_frame = FALSE;
	self->start_code = -1;
}

int plm_video_has_ended(plm_video_t *self) {
	return plm_buffer_has_ended(self->buffer);
}

plm_frame_t *plm_video_decode(plm_video_t *self) {
	if (!plm_video_has_header(self)) {
		return NULL;
	}
	
	plm_frame_t *frame = NULL;
	do {
		if (self->start_code != PLM_START_PICTURE) {
			self->start_code = plm_buffer_find_start_code(self->buffer, PLM_START_PICTURE);
			if (self->start_code == -1) {
				return NULL;
			}
		}

		// Make sure we have a full picture in the buffer before attempting to
		// decode it. Sadly, this can only be done by seeking for the start code
		// of the next picture. Also, if we didn't find the start code for the
		// next picture, but the source has ended, we assume that this last
		// picture is in the buffer.
		if (
			plm_buffer_has_start_code(self->buffer, PLM_START_PICTURE) == -1 &&
			!plm_buffer_has_ended(self->buffer)
		) {
			return NULL;
		}
		
		plm_video_decode_picture(self);

		if (self->assume_no_b_frames) {
			frame = &self->frame_backward;
		}
		else if (self->picture_type == PLM_VIDEO_PICTURE_TYPE_B) {
			frame = &self->frame_current;
		}
		else if (self->has_reference_frame) {
			frame = &self->frame_forward;
		}
		else {
			self->has_reference_frame = TRUE;
		}
	} while (!frame);
	
	frame->time = self->time;
	self->frames_decoded++;
	self->time = (double)self->frames_decoded / self->framerate;
	
	return frame;
}

int plm_video_has_header(plm_video_t *self) {
	if (self->has_sequence_header) {
		return TRUE;
	}

	if (self->start_code != PLM_START_SEQUENCE) {
		self->start_code = plm_buffer_find_start_code(self->buffer, PLM_START_SEQUENCE);
	}
	if (self->start_code == -1) {
		return FALSE;
	}
	
	if (!plm_video_decode_sequence_header(self)) {
		return FALSE;
	}

	return TRUE;
}

int plm_video_decode_sequence_header(plm_video_t *self) {
	int max_header_size = 64 + 2 * 64 * 8; // 64 bit header + 2x 64 byte matrix
	if (!plm_buffer_has(self->buffer, max_header_size)) {
		return FALSE;
	}

	self->width = plm_buffer_read(self->buffer, 12);
	self->height = plm_buffer_read(self->buffer, 12);

	if (self->width <= 0 || self->height <= 0) {
		return FALSE;
	}

	// Skip pixel aspect ratio
	plm_buffer_skip(self->buffer, 4);

	self->framerate = PLM_VIDEO_PICTURE_RATE[plm_buffer_read(self->buffer, 4)];

	// Skip bit_rate, marker, buffer_size and constrained bit
	plm_buffer_skip(self->buffer, 18 + 1 + 10 + 1);

	// Load custom intra quant matrix?
	if (plm_buffer_read(self->buffer, 1)) { 
		for (int i = 0; i < 64; i++) {
			int idx = PLM_VIDEO_ZIG_ZAG[i];
			self->intra_quant_matrix[idx] = plm_buffer_read(self->buffer, 8);
		}
	}
	else {
		memcpy(self->intra_quant_matrix, PLM_VIDEO_INTRA_QUANT_MATRIX, 64);
	}

	// Load custom non intra quant matrix?
	if (plm_buffer_read(self->buffer, 1)) { 
		for (int i = 0; i < 64; i++) {
			int idx = PLM_VIDEO_ZIG_ZAG[i];
			self->non_intra_quant_matrix[idx] = plm_buffer_read(self->buffer, 8);
		}
	}
	else {
		memcpy(self->non_intra_quant_matrix, PLM_VIDEO_NON_INTRA_QUANT_MATRIX, 64);
	}

	self->mb_width = (self->width + 15) >> 4;
	self->mb_height = (self->height + 15) >> 4;
	self->mb_size = self->mb_width * self->mb_height;

	self->luma_width = self->mb_width << 4;
	self->luma_height = self->mb_height << 4;

	self->chroma_width = self->mb_width << 3;
	self->chroma_height = self->mb_height << 3;


	// Allocate one big chunk of data for all 3 frames = 9 planes
	size_t luma_plane_size = self->luma_width * self->luma_height;
	size_t chroma_plane_size = self->chroma_width * self->chroma_height;
	size_t frame_data_size = (luma_plane_size + 2 * chroma_plane_size);

	self->frames_data = (uint8_t*)malloc(frame_data_size * 3);
	plm_video_init_frame(self, &self->frame_current, self->frames_data + frame_data_size * 0);
	plm_video_init_frame(self, &self->frame_forward, self->frames_data + frame_data_size * 1);
	plm_video_init_frame(self, &self->frame_backward, self->frames_data + frame_data_size * 2);

	self->has_sequence_header = TRUE;
	return TRUE;
}

void plm_video_init_frame(plm_video_t *self, plm_frame_t *frame, uint8_t *base) {
	size_t luma_plane_size = self->luma_width * self->luma_height;
	size_t chroma_plane_size = self->chroma_width * self->chroma_height;

	frame->width = self->width;
	frame->height = self->height;
	frame->y.width = self->luma_width;
	frame->y.height = self->luma_height;
	frame->y.data = base;

	frame->cr.width = self->chroma_width;
	frame->cr.height = self->chroma_height;
	frame->cr.data = base + luma_plane_size;

	frame->cb.width = self->chroma_width;
	frame->cb.height = self->chroma_height;
	frame->cb.data = base + luma_plane_size + chroma_plane_size;
}

void plm_video_decode_picture(plm_video_t *self) {
	plm_buffer_skip(self->buffer, 10); // skip temporalReference
	self->picture_type = plm_buffer_read(self->buffer, 3);
	plm_buffer_skip(self->buffer, 16); // skip vbv_delay

	// D frames or unknown coding type
	if (self->picture_type <= 0 || self->picture_type > PLM_VIDEO_PICTURE_TYPE_B) {
		return;
	}

	// Forward full_px, f_code
	if (
		self->picture_type == PLM_VIDEO_PICTURE_TYPE_PREDICTIVE ||
		self->picture_type == PLM_VIDEO_PICTURE_TYPE_B
	) {
		self->motion_forward.full_px = plm_buffer_read(self->buffer, 1);
		int f_code = plm_buffer_read(self->buffer, 3);
		if (f_code == 0) {
			// Ignore picture with zero f_code
			return;
		}
		self->motion_forward.r_size = f_code - 1;
	}

	// Backward full_px, f_code
	if (self->picture_type == PLM_VIDEO_PICTURE_TYPE_B) {
		self->motion_backward.full_px = plm_buffer_read(self->buffer, 1);
		int f_code = plm_buffer_read(self->buffer, 3);
		if (f_code == 0) {
			// Ignore picture with zero f_code
			return;
		}
		self->motion_backward.r_size = f_code - 1;
	}

	plm_frame_t frame_temp = self->frame_forward;
	if (
		self->picture_type == PLM_VIDEO_PICTURE_TYPE_INTRA ||
		self->picture_type == PLM_VIDEO_PICTURE_TYPE_PREDICTIVE
	) {
		self->frame_forward = self->frame_backward;
	}


	// Find the first slice; this skips extension and user data
	do {
		self->start_code = plm_buffer_next_start_code(self->buffer);
	} while (!PLM_START_IS_SLICE(self->start_code));

	// Decode all slices
	do {
		plm_video_decode_slice(self, self->start_code & 0x000000FF);
		if (self->macroblock_address == self->mb_size - 1) {
			break;
		}
		self->start_code = plm_buffer_next_start_code(self->buffer);
	} while (PLM_START_IS_SLICE(self->start_code));

	// If this is a reference picture rotate the prediction pointers
	if (
		self->picture_type == PLM_VIDEO_PICTURE_TYPE_INTRA ||
		self->picture_type == PLM_VIDEO_PICTURE_TYPE_PREDICTIVE
	) {
		self->frame_backward = self->frame_current;
		self->frame_current = frame_temp;
	}
}

void plm_video_decode_slice(plm_video_t *self, int slice) {
	self->slice_begin = TRUE;
	self->macroblock_address = (slice - 1) * self->mb_width - 1;

	// Reset motion vectors and DC predictors
	self->motion_backward.h = self->motion_forward.h = 0;
	self->motion_backward.v = self->motion_forward.v = 0;
	self->dc_predictor[0] = 128;
	self->dc_predictor[1] = 128;
	self->dc_predictor[2] = 128;

	self->quantizer_scale = plm_buffer_read(self->buffer, 5);

	// Skip extra
	while (plm_buffer_read(self->buffer, 1)) {
		plm_buffer_skip(self->buffer, 8);
	}

	do {
		plm_video_decode_macroblock(self);
	} while (
		self->macroblock_address < self->mb_size - 1 &&
		plm_buffer_no_start_code(self->buffer)
	);
}

void plm_video_decode_macroblock(plm_video_t *self) {
	// Decode self->macroblock_address_increment
	int increment = 0;
	int t = plm_buffer_read_vlc(self->buffer, PLM_VIDEO_MACROBLOCK_ADDRESS_INCREMENT);

	while (t == 34) {
		// macroblock_stuffing
		t = plm_buffer_read_vlc(self->buffer, PLM_VIDEO_MACROBLOCK_ADDRESS_INCREMENT);
	}
	while (t == 35) {
		// macroblock_escape
		increment += 33;
		t = plm_buffer_read_vlc(self->buffer, PLM_VIDEO_MACROBLOCK_ADDRESS_INCREMENT);
	}
	increment += t;

	// Process any skipped macroblocks
	if (self->slice_begin) {
		// The first self->macroblock_address_increment of each slice is relative
		// to beginning of the preverious row, not the preverious macroblock
		self->slice_begin = FALSE;
		self->macroblock_address += increment;
	}
	else {
		if (self->macroblock_address + increment >= self->mb_size) {
			return; // invalid
		}
		if (increment > 1) {
			// Skipped macroblocks reset DC predictors
			self->dc_predictor[0] = 128;
			self->dc_predictor[1] = 128;
			self->dc_predictor[2] = 128;

			// Skipped macroblocks in P-pictures reset motion vectors
			if (self->picture_type == PLM_VIDEO_PICTURE_TYPE_PREDICTIVE) {
				self->motion_forward.h = 0;
				self->motion_forward.v = 0;
			}
		}

		// Predict skipped macroblocks
		while (increment > 1) {
			self->macroblock_address++;
			self->mb_row = self->macroblock_address / self->mb_width;
			self->mb_col = self->macroblock_address % self->mb_width;

			plm_video_predict_macroblock(self);
			increment--;
		}
		self->macroblock_address++;
	}

	self->mb_row = self->macroblock_address / self->mb_width;
	self->mb_col = self->macroblock_address % self->mb_width;

	if (self->mb_col >= self->mb_width || self->mb_row >= self->mb_height) {
		return; // corrupt stream;
	}

	// Process the current macroblock
	const plm_vlc_t *table = PLM_VIDEO_MACROBLOCK_TYPE[self->picture_type];
	self->macroblock_type = plm_buffer_read_vlc(self->buffer, table);

	self->macroblock_intra = (self->macroblock_type & 0x01);
	self->motion_forward.is_set = (self->macroblock_type & 0x08);
	self->motion_backward.is_set = (self->macroblock_type & 0x04);

	// Quantizer scale
	if ((self->macroblock_type & 0x10) != 0) {
		self->quantizer_scale = plm_buffer_read(self->buffer, 5);
	}

	if (self->macroblock_intra) {
		// Intra-coded macroblocks reset motion vectors
		self->motion_backward.h = self->motion_forward.h = 0;
		self->motion_backward.v = self->motion_forward.v = 0;
	}
	else {
		// Non-intra macroblocks reset DC predictors
		self->dc_predictor[0] = 128;
		self->dc_predictor[1] = 128;
		self->dc_predictor[2] = 128;

		plm_video_decode_motion_vectors(self);
		plm_video_predict_macroblock(self);
	}

	// Decode blocks
	int cbp = ((self->macroblock_type & 0x02) != 0)
		? plm_buffer_read_vlc(self->buffer, PLM_VIDEO_CODE_BLOCK_PATTERN)
		: (self->macroblock_intra ? 0x3f : 0);

	for (int block = 0, mask = 0x20; block < 6; block++) {
		if ((cbp & mask) != 0) {
			plm_video_decode_block(self, block);
		}
		mask >>= 1;
	}
}

void plm_video_decode_motion_vectors(plm_video_t *self) {

	// Forward
	if (self->motion_forward.is_set) {
		int r_size = self->motion_forward.r_size;
		self->motion_forward.h = plm_video_decode_motion_vector(self, r_size, self->motion_forward.h);
		self->motion_forward.v = plm_video_decode_motion_vector(self, r_size, self->motion_forward.v);
	}
	else if (self->picture_type == PLM_VIDEO_PICTURE_TYPE_PREDICTIVE) {
		// No motion information in P-picture, reset vectors
		self->motion_forward.h = 0;
		self->motion_forward.v = 0;
	}

	if (self->motion_backward.is_set) {
		int r_size = self->motion_backward.r_size;
		self->motion_backward.h = plm_video_decode_motion_vector(self, r_size, self->motion_backward.h);
		self->motion_backward.v = plm_video_decode_motion_vector(self, r_size, self->motion_backward.v);
	}
}

int plm_video_decode_motion_vector(plm_video_t *self, int r_size, int motion) {
	int fscale = 1 << r_size;
	int m_code = plm_buffer_read_vlc(self->buffer, PLM_VIDEO_MOTION);
	int r = 0;
	int d;

	if ((m_code != 0) && (fscale != 1)) {
		r = plm_buffer_read(self->buffer, r_size);
		d = ((abs(m_code) - 1) << r_size) + r + 1;
		if (m_code < 0) {
			d = -d;
		}
	}
	else {
		d = m_code;
	}

	motion += d;
	if (motion >(fscale << 4) - 1) {
		motion -= fscale << 5;
	}
	else if (motion < ((-fscale) << 4)) {
		motion += fscale << 5;
	}

	return motion;
}

void plm_video_predict_macroblock(plm_video_t *self) {
	int fw_h = self->motion_forward.h;
	int fw_v = self->motion_forward.v;

	if (self->motion_forward.full_px) {
		fw_h <<= 1;
		fw_v <<= 1;
	}

	if (self->picture_type == PLM_VIDEO_PICTURE_TYPE_B) {
		int bw_h = self->motion_backward.h;
		int bw_v = self->motion_backward.v;

		if (self->motion_backward.full_px) {
			bw_h <<= 1;
			bw_v <<= 1;
		}

		if (self->motion_forward.is_set) {
			plm_video_copy_macroblock(self, fw_h, fw_v, &self->frame_forward);
			if (self->motion_backward.is_set) {
				plm_video_interpolate_macroblock(self, bw_h, bw_v, &self->frame_backward);
			}
		}
		else {
			plm_video_copy_macroblock(self, bw_h, bw_v, &self->frame_backward);
		}
	}
	else {
		plm_video_copy_macroblock(self, fw_h, fw_v, &self->frame_forward);
	}
}

void plm_video_copy_macroblock(plm_video_t *self, int motion_h, int motion_v, plm_frame_t *d) {
	plm_frame_t *s = &self->frame_current;
	plm_video_process_macroblock(self, s->y.data, d->y.data, motion_h, motion_v, 16, FALSE);
	plm_video_process_macroblock(self, s->cr.data, d->cr.data, motion_h / 2, motion_v / 2, 8, FALSE);
	plm_video_process_macroblock(self, s->cb.data, d->cb.data, motion_h / 2, motion_v / 2, 8, FALSE);
}

void plm_video_interpolate_macroblock(plm_video_t *self, int motion_h, int motion_v, plm_frame_t *d) {
	plm_frame_t *s = &self->frame_current;
	plm_video_process_macroblock(self, s->y.data, d->y.data, motion_h, motion_v, 16, TRUE);
	plm_video_process_macroblock(self, s->cr.data, d->cr.data, motion_h / 2, motion_v / 2, 8, TRUE);
	plm_video_process_macroblock(self, s->cb.data, d->cb.data, motion_h / 2, motion_v / 2, 8, TRUE);
}

#define PLM_BLOCK_SET(DEST, DEST_INDEX, DEST_WIDTH, SOURCE_INDEX, SOURCE_WIDTH, BLOCK_SIZE, OP) do { \
	int dest_scan = DEST_WIDTH - BLOCK_SIZE; \
	int source_scan = SOURCE_WIDTH - BLOCK_SIZE; \
	for (int y = 0; y < BLOCK_SIZE; y++) { \
		for (int x = 0; x < BLOCK_SIZE; x++) { \
			DEST[DEST_INDEX] = OP; \
			SOURCE_INDEX++; DEST_INDEX++; \
		} \
		SOURCE_INDEX += source_scan; \
		DEST_INDEX += dest_scan; \
	}} while(FALSE)

void plm_video_process_macroblock(
	plm_video_t *self, uint8_t *d, uint8_t *s,
	int motion_h, int motion_v, int block_size, int interpolate
) {
	int dw = self->mb_width * block_size;

	int hp = motion_h >> 1;
	int vp = motion_v >> 1;
	int odd_h = (motion_h & 1) == 1;
	int odd_v = (motion_v & 1) == 1;

	unsigned int si = ((self->mb_row * block_size) + vp) * dw + (self->mb_col * block_size) + hp;
	unsigned int di = (self->mb_row * dw + self->mb_col) * block_size;
	
	unsigned int max_address = (dw * (self->mb_height * block_size - block_size + 1) - block_size);
	if (si > max_address || di > max_address) {
		return; // corrupt video
	}

	#define PLM_MB_CASE(INTERPOLATE, ODD_H, ODD_V, OP) \
		case ((INTERPOLATE << 2) | (ODD_H << 1) | (ODD_V)): \
			PLM_BLOCK_SET(d, di, dw, si, dw, block_size, OP); \
			break

	switch ((interpolate << 2) | (odd_h << 1) | (odd_v)) {
		PLM_MB_CASE(0, 0, 0, (s[si]));
		PLM_MB_CASE(0, 0, 1, (s[si] + s[si + dw] + 1) >> 1);
		PLM_MB_CASE(0, 1, 0, (s[si] + s[si + 1] + 1) >> 1);
		PLM_MB_CASE(0, 1, 1, (s[si] + s[si + 1] + s[si + dw] + s[si + dw + 1] + 2) >> 2);

		PLM_MB_CASE(1, 0, 0, (d[di] + (s[si]) + 1) >> 1);
		PLM_MB_CASE(1, 0, 1, (d[di] + ((s[si] + s[si + dw] + 1) >> 1) + 1) >> 1);
		PLM_MB_CASE(1, 1, 0, (d[di] + ((s[si] + s[si + 1] + 1) >> 1) + 1) >> 1);
		PLM_MB_CASE(1, 1, 1, (d[di] + ((s[si] + s[si + 1] + s[si + dw] + s[si + dw + 1] + 2) >> 2) + 1) >> 1);
	}

	#undef PLM_MB_CASE
}

void plm_video_decode_block(plm_video_t *self, int block) {

	int n = 0;
	uint8_t *quant_matrix;

	// Decode DC coefficient of intra-coded blocks
	if (self->macroblock_intra) {
		int predictor;
		int dct_size;

		// DC prediction
		int plane_index = block > 3 ? block - 3 : 0;
		predictor = self->dc_predictor[plane_index];
		dct_size = plm_buffer_read_vlc(self->buffer, PLM_VIDEO_DCT_SIZE[plane_index]);

		// Read DC coeff
		if (dct_size > 0) {
			int differential = plm_buffer_read(self->buffer, dct_size);
			if ((differential & (1 << (dct_size - 1))) != 0) {
				self->block_data[0] = predictor + differential;
			}
			else {
				self->block_data[0] = predictor + (-(1 << dct_size) | (differential + 1));
			}
		}
		else {
			self->block_data[0] = predictor;
		}

		// Save predictor value
		self->dc_predictor[plane_index] = self->block_data[0];

		// Dequantize + premultiply
		self->block_data[0] <<= (3 + 5);

		quant_matrix = self->intra_quant_matrix;
		n = 1;
	}
	else {
		quant_matrix = self->non_intra_quant_matrix;
	}

	// Decode AC coefficients (+DC for non-intra)
	int level = 0;
	while (TRUE) {
		int run = 0;
		uint16_t coeff = plm_buffer_read_vlc_uint(self->buffer, PLM_VIDEO_DCT_COEFF);

		if ((coeff == 0x0001) && (n > 0) && (plm_buffer_read(self->buffer, 1) == 0)) {
			// end_of_block
			break;
		}
		if (coeff == 0xffff) {
			// escape
			run = plm_buffer_read(self->buffer, 6);
			level = plm_buffer_read(self->buffer, 8);
			if (level == 0) {
				level = plm_buffer_read(self->buffer, 8);
			}
			else if (level == 128) {
				level = plm_buffer_read(self->buffer, 8) - 256;
			}
			else if (level > 128) {
				level = level - 256;
			}
		}
		else {
			run = coeff >> 8;
			level = coeff & 0xff;
			if (plm_buffer_read(self->buffer, 1)) {
				level = -level;
			}
		}

		n += run;
		if (n < 0 || n >= 64) {
			return; // invalid
		}

		int de_zig_zagged = PLM_VIDEO_ZIG_ZAG[n];
		n++;

		// Dequantize, oddify, clip
		level <<= 1;
		if (!self->macroblock_intra) {
			level += (level < 0 ? -1 : 1);
		}
		level = (level * self->quantizer_scale * quant_matrix[de_zig_zagged]) >> 4;
		if ((level & 1) == 0) {
			level -= level > 0 ? 1 : -1;
		}
		if (level > 2047) {
			level = 2047;
		}
		else if (level < -2048) {
			level = -2048;
		}

		// Save premultiplied coefficient
		self->block_data[de_zig_zagged] = level * PLM_VIDEO_PREMULTIPLIER_MATRIX[de_zig_zagged];
	}

	// Move block to its place
	uint8_t *d;
	int dw;
	int di;

	if (block < 4) {
		d = self->frame_current.y.data;
		dw = self->luma_width;
		di = (self->mb_row * self->luma_width + self->mb_col) << 4;
		if ((block & 1) != 0) {
			di += 8;
		}
		if ((block & 2) != 0) {
			di += self->luma_width << 3;
		}
	}
	else {
		d = (block == 4) ? self->frame_current.cb.data : self->frame_current.cr.data;
		dw = self->chroma_width;
		di = ((self->mb_row * self->luma_width) << 2) + (self->mb_col << 3);
	}

	int *s = self->block_data;
	int si = 0;
	if (self->macroblock_intra) {
		// Overwrite (no prediction)
		if (n == 1) {
			int clamped = plm_clamp((s[0] + 128) >> 8);
			PLM_BLOCK_SET(d, di, dw, si, 8, 8, clamped);
			s[0] = 0;
		}
		else {
			plm_video_idct(s);
			PLM_BLOCK_SET(d, di, dw, si, 8, 8, plm_clamp(s[si]));
			memset(self->block_data, 0, sizeof(self->block_data));
		}
	}
	else {
		// Add data to the predicted macroblock
		if (n == 1) {
			int value = (s[0] + 128) >> 8;
			PLM_BLOCK_SET(d, di, dw, si, 8, 8, plm_clamp(d[di] + value));
			s[0] = 0;
		}
		else {
			plm_video_idct(s);
			PLM_BLOCK_SET(d, di, dw, si, 8, 8, plm_clamp(d[di] + s[si]));
			memset(self->block_data, 0, sizeof(self->block_data));
		}
	}
}

void plm_video_idct(int *block) {
	int
		b1, b3, b4, b6, b7, tmp1, tmp2, m0,
		x0, x1, x2, x3, x4, y3, y4, y5, y6, y7;

	// Transform columns
	for (int i = 0; i < 8; ++i) {
		b1 = block[4 * 8 + i];
		b3 = block[2 * 8 + i] + block[6 * 8 + i];
		b4 = block[5 * 8 + i] - block[3 * 8 + i];
		tmp1 = block[1 * 8 + i] + block[7 * 8 + i];
		tmp2 = block[3 * 8 + i] + block[5 * 8 + i];
		b6 = block[1 * 8 + i] - block[7 * 8 + i];
		b7 = tmp1 + tmp2;
		m0 = block[0 * 8 + i];
		x4 = ((b6 * 473 - b4 * 196 + 128) >> 8) - b7;
		x0 = x4 - (((tmp1 - tmp2) * 362 + 128) >> 8);
		x1 = m0 - b1;
		x2 = (((block[2 * 8 + i] - block[6 * 8 + i]) * 362 + 128) >> 8) - b3;
		x3 = m0 + b1;
		y3 = x1 + x2;
		y4 = x3 + b3;
		y5 = x1 - x2;
		y6 = x3 - b3;
		y7 = -x0 - ((b4 * 473 + b6 * 196 + 128) >> 8);
		block[0 * 8 + i] = b7 + y4;
		block[1 * 8 + i] = x4 + y3;
		block[2 * 8 + i] = y5 - x0;
		block[3 * 8 + i] = y6 - y7;
		block[4 * 8 + i] = y6 + y7;
		block[5 * 8 + i] = x0 + y5;
		block[6 * 8 + i] = y3 - x4;
		block[7 * 8 + i] = y4 - b7;
	}

	// Transform rows
	for (int i = 0; i < 64; i += 8) {
		b1 = block[4 + i];
		b3 = block[2 + i] + block[6 + i];
		b4 = block[5 + i] - block[3 + i];
		tmp1 = block[1 + i] + block[7 + i];
		tmp2 = block[3 + i] + block[5 + i];
		b6 = block[1 + i] - block[7 + i];
		b7 = tmp1 + tmp2;
		m0 = block[0 + i];
		x4 = ((b6 * 473 - b4 * 196 + 128) >> 8) - b7;
		x0 = x4 - (((tmp1 - tmp2) * 362 + 128) >> 8);
		x1 = m0 - b1;
		x2 = (((block[2 + i] - block[6 + i]) * 362 + 128) >> 8) - b3;
		x3 = m0 + b1;
		y3 = x1 + x2;
		y4 = x3 + b3;
		y5 = x1 - x2;
		y6 = x3 - b3;
		y7 = -x0 - ((b4 * 473 + b6 * 196 + 128) >> 8);
		block[0 + i] = (b7 + y4 + 128) >> 8;
		block[1 + i] = (x4 + y3 + 128) >> 8;
		block[2 + i] = (y5 - x0 + 128) >> 8;
		block[3 + i] = (y6 - y7 + 128) >> 8;
		block[4 + i] = (y6 + y7 + 128) >> 8;
		block[5 + i] = (x0 + y5 + 128) >> 8;
		block[6 + i] = (y3 - x4 + 128) >> 8;
		block[7 + i] = (y4 - b7 + 128) >> 8;
	}
}

#define PLM_PUT_PIXEL(RI, GI, BI, Y_OFFSET, DEST_OFFSET) \
	y = frame->y.data[y_index + Y_OFFSET]; \
	dest[d_index + DEST_OFFSET + RI] = plm_clamp(y + r); \
	dest[d_index + DEST_OFFSET + GI] = plm_clamp(y - g); \
	dest[d_index + DEST_OFFSET + BI] = plm_clamp(y + b);

#define PLM_DEFINE_FRAME_CONVERT_FUNCTION(NAME, BYTES_PER_PIXEL, RI, GI, BI) \
	void NAME(plm_frame_t *frame, uint8_t *dest, int stride) { \
		int cols = frame->width >> 1; \
		int rows = frame->height >> 1; \
		int yw = frame->y.width; \
		int cw = frame->cb.width; \
		for (int row = 0; row < rows; row++) { \
			int c_index = row * cw; \
			int y_index = row * 2 * yw; \
			int d_index = row * 2 * stride; \
			for (int col = 0; col < cols; col++) { \
				int y; \
				int cr = frame->cr.data[c_index]; \
				int cb = frame->cb.data[c_index]; \
				int r = (cr + ((cr * 103) >> 8)) - 179; \
				int g = ((cb * 88) >> 8) - 44 + ((cr * 183) >> 8) - 91; \
				int b = (cb + ((cb * 198) >> 8)) - 227; \
				PLM_PUT_PIXEL(RI, GI, BI, 0,      0); \
				PLM_PUT_PIXEL(RI, GI, BI, 1,      BYTES_PER_PIXEL); \
				PLM_PUT_PIXEL(RI, GI, BI, yw,     stride); \
				PLM_PUT_PIXEL(RI, GI, BI, yw + 1, stride + BYTES_PER_PIXEL); \
				c_index += 1; \
				y_index += 2; \
				d_index += 2 * BYTES_PER_PIXEL; \
			} \
		} \
	}

PLM_DEFINE_FRAME_CONVERT_FUNCTION(plm_frame_to_rgb,  3, 0, 1, 2)
PLM_DEFINE_FRAME_CONVERT_FUNCTION(plm_frame_to_bgr,  3, 2, 1, 0)
PLM_DEFINE_FRAME_CONVERT_FUNCTION(plm_frame_to_rgba, 4, 0, 1, 2)
PLM_DEFINE_FRAME_CONVERT_FUNCTION(plm_frame_to_bgra, 4, 2, 1, 0)
PLM_DEFINE_FRAME_CONVERT_FUNCTION(plm_frame_to_argb, 4, 1, 2, 3)
PLM_DEFINE_FRAME_CONVERT_FUNCTION(plm_frame_to_abgr, 4, 3, 2, 1)


#undef PLM_PUT_PIXEL
#undef PLM_DEFINE_FRAME_CONVERT_FUNCTION



// -----------------------------------------------------------------------------
// plm_audio implementation

// Based on kjmp2 by Martin J. Fiedler
// http://keyj.emphy.de/kjmp2/

static const int PLM_AUDIO_FRAME_SYNC = 0x7ff;

static const int PLM_AUDIO_MPEG_2_5 = 0x0;
static const int PLM_AUDIO_MPEG_2 = 0x2;
static const int PLM_AUDIO_MPEG_1 = 0x3;

static const int PLM_AUDIO_LAYER_III = 0x1;
static const int PLM_AUDIO_LAYER_II = 0x2;
static const int PLM_AUDIO_LAYER_I = 0x3;

static const int PLM_AUDIO_MODE_STEREO = 0x0;
static const int PLM_AUDIO_MODE_JOINT_STEREO = 0x1;
static const int PLM_AUDIO_MODE_DUAL_CHANNEL = 0x2;
static const int PLM_AUDIO_MODE_MONO = 0x3;

static const unsigned short PLM_AUDIO_SAMPLE_RATE[] = {
	44100, 48000, 32000, 0, // MPEG-1
	22050, 24000, 16000, 0  // MPEG-2
};

static const short PLM_AUDIO_BIT_RATE[] = {
	32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, // MPEG-1
	 8, 16, 24, 32, 40, 48,  56,  64,  80,  96, 112, 128, 144, 160  // MPEG-2
};

static const int PLM_AUDIO_SCALEFACTOR_BASE[] = {
	0x02000000, 0x01965FEA, 0x01428A30
};

static const float PLM_AUDIO_SYNTHESIS_WINDOW[] = {
	     0.0,     -0.5,     -0.5,     -0.5,     -0.5,     -0.5,
	    -0.5,     -1.0,     -1.0,     -1.0,     -1.0,     -1.5,
	    -1.5,     -2.0,     -2.0,     -2.5,     -2.5,     -3.0,
	    -3.5,     -3.5,     -4.0,     -4.5,     -5.0,     -5.5,
	    -6.5,     -7.0,     -8.0,     -8.5,     -9.5,    -10.5,
	   -12.0,    -13.0,    -14.5,    -15.5,    -17.5,    -19.0,
	   -20.5,    -22.5,    -24.5,    -26.5,    -29.0,    -31.5,
	   -34.0,    -36.5,    -39.5,    -42.5,    -45.5,    -48.5,
	   -52.0,    -55.5,    -58.5,    -62.5,    -66.0,    -69.5,
	   -73.5,    -77.0,    -80.5,    -84.5,    -88.0,    -91.5,
	   -95.0,    -98.0,   -101.0,   -104.0,    106.5,    109.0,
	   111.0,    112.5,    113.5,    114.0,    114.0,    113.5,
	   112.0,    110.5,    107.5,    104.0,    100.0,     94.5,
	    88.5,     81.5,     73.0,     63.5,     53.0,     41.5,
	    28.5,     14.5,     -1.0,    -18.0,    -36.0,    -55.5,
	   -76.5,    -98.5,   -122.0,   -147.0,   -173.5,   -200.5,
	  -229.5,   -259.5,   -290.5,   -322.5,   -355.5,   -389.5,
	  -424.0,   -459.5,   -495.5,   -532.0,   -568.5,   -605.0,
	  -641.5,   -678.0,   -714.0,   -749.0,   -783.5,   -817.0,
	  -849.0,   -879.5,   -908.5,   -935.0,   -959.5,   -981.0,
	 -1000.5,  -1016.0,  -1028.5,  -1037.5,  -1042.5,  -1043.5,
	 -1040.0,  -1031.5,   1018.5,   1000.0,    976.0,    946.5,
	   911.0,    869.5,    822.0,    767.5,    707.0,    640.0,
	   565.5,    485.0,    397.0,    302.5,    201.0,     92.5,
	   -22.5,   -144.0,   -272.5,   -407.0,   -547.5,   -694.0,
	  -846.0,  -1003.0,  -1165.0,  -1331.5,  -1502.0,  -1675.5,
	 -1852.5,  -2031.5,  -2212.5,  -2394.0,  -2576.5,  -2758.5,
	 -2939.5,  -3118.5,  -3294.5,  -3467.5,  -3635.5,  -3798.5,
	 -3955.0,  -4104.5,  -4245.5,  -4377.5,  -4499.0,  -4609.5,
	 -4708.0,  -4792.5,  -4863.5,  -4919.0,  -4958.0,  -4979.5,
	 -4983.0,  -4967.5,  -4931.5,  -4875.0,  -4796.0,  -4694.5,
	 -4569.5,  -4420.0,  -4246.0,  -4046.0,  -3820.0,  -3567.0,
	  3287.0,   2979.5,   2644.0,   2280.5,   1888.0,   1467.5,
	  1018.5,    541.0,     35.0,   -499.0,  -1061.0,  -1650.0,
	 -2266.5,  -2909.0,  -3577.0,  -4270.0,  -4987.5,  -5727.5,
	 -6490.0,  -7274.0,  -8077.5,  -8899.5,  -9739.0, -10594.5,
	-11464.5, -12347.0, -13241.0, -14144.5, -15056.0, -15973.5,
	-16895.5, -17820.0, -18744.5, -19668.0, -20588.0, -21503.0,
	-22410.5, -23308.5, -24195.0, -25068.5, -25926.5, -26767.0,
	-27589.0, -28389.0, -29166.5, -29919.0, -30644.5, -31342.0,
	-32009.5, -32645.0, -33247.0, -33814.5, -34346.0, -34839.5,
	-35295.0, -35710.0, -36084.5, -36417.5, -36707.5, -36954.0,
	-37156.5, -37315.0, -37428.0, -37496.0,  37519.0,  37496.0,
	 37428.0,  37315.0,  37156.5,  36954.0,  36707.5,  36417.5,
	 36084.5,  35710.0,  35295.0,  34839.5,  34346.0,  33814.5,
	 33247.0,  32645.0,  32009.5,  31342.0,  30644.5,  29919.0,
	 29166.5,  28389.0,  27589.0,  26767.0,  25926.5,  25068.5,
	 24195.0,  23308.5,  22410.5,  21503.0,  20588.0,  19668.0,
	 18744.5,  17820.0,  16895.5,  15973.5,  15056.0,  14144.5,
	 13241.0,  12347.0,  11464.5,  10594.5,   9739.0,   8899.5,
	  8077.5,   7274.0,   6490.0,   5727.5,   4987.5,   4270.0,
	  3577.0,   2909.0,   2266.5,   1650.0,   1061.0,    499.0,
	   -35.0,   -541.0,  -1018.5,  -1467.5,  -1888.0,  -2280.5,
	 -2644.0,  -2979.5,   3287.0,   3567.0,   3820.0,   4046.0,
	  4246.0,   4420.0,   4569.5,   4694.5,   4796.0,   4875.0,
	  4931.5,   4967.5,   4983.0,   4979.5,   4958.0,   4919.0,
	  4863.5,   4792.5,   4708.0,   4609.5,   4499.0,   4377.5,
	  4245.5,   4104.5,   3955.0,   3798.5,   3635.5,   3467.5,
	  3294.5,   3118.5,   2939.5,   2758.5,   2576.5,   2394.0,
	  2212.5,   2031.5,   1852.5,   1675.5,   1502.0,   1331.5,
	  1165.0,   1003.0,    846.0,    694.0,    547.5,    407.0,
	   272.5,    144.0,     22.5,    -92.5,   -201.0,   -302.5,
	  -397.0,   -485.0,   -565.5,   -640.0,   -707.0,   -767.5,
	  -822.0,   -869.5,   -911.0,   -946.5,   -976.0,  -1000.0,
	  1018.5,   1031.5,   1040.0,   1043.5,   1042.5,   1037.5,
	  1028.5,   1016.0,   1000.5,    981.0,    959.5,    935.0,
	   908.5,    879.5,    849.0,    817.0,    783.5,    749.0,
	   714.0,    678.0,    641.5,    605.0,    568.5,    532.0,
	   495.5,    459.5,    424.0,    389.5,    355.5,    322.5,
	   290.5,    259.5,    229.5,    200.5,    173.5,    147.0,
	   122.0,     98.5,     76.5,     55.5,     36.0,     18.0,
	     1.0,    -14.5,    -28.5,    -41.5,    -53.0,    -63.5,
	   -73.0,    -81.5,    -88.5,    -94.5,   -100.0,   -104.0,
	  -107.5,   -110.5,   -112.0,   -113.5,   -114.0,   -114.0,
	  -113.5,   -112.5,   -111.0,   -109.0,    106.5,    104.0,
	   101.0,     98.0,     95.0,     91.5,     88.0,     84.5,
	    80.5,     77.0,     73.5,     69.5,     66.0,     62.5,
	    58.5,     55.5,     52.0,     48.5,     45.5,     42.5,
	    39.5,     36.5,     34.0,     31.5,     29.0,     26.5,
	    24.5,     22.5,     20.5,     19.0,     17.5,     15.5,
	    14.5,     13.0,     12.0,     10.5,      9.5,      8.5,
	     8.0,      7.0,      6.5,      5.5,      5.0,      4.5,
	     4.0,      3.5,      3.5,      3.0,      2.5,      2.5,
	     2.0,      2.0,      1.5,      1.5,      1.0,      1.0,
	     1.0,      1.0,      0.5,      0.5,      0.5,      0.5,
	     0.5,      0.5
};

// Quantizer lookup, step 1: bitrate classes
static const uint8_t PLM_AUDIO_QUANT_LUT_STEP_1[2][16] = {
	// 32, 48, 56, 64, 80, 96,112,128,160,192,224,256,320,384 <- bitrate
	{ 0,  0,  1,  1,  1,  2,  2,  2,  2,  2,  2,  2,  2,  2 }, // mono
	// 16, 24, 28, 32, 40, 48, 56, 64, 80, 96,112,128,160,192 <- bitrate / chan
	{ 0,  0,  0,  0,  0,  0,  1,  1,  1,  2,  2,  2,  2,  2 } // stereo
};

// Quantizer lookup, step 2: bitrate class, sample rate -> B2 table idx, sblimit
static const uint8_t PLM_AUDIO_QUANT_TAB_A = (27 | 64);   // Table 3-B.2a: high-rate, sblimit = 27
static const uint8_t PLM_AUDIO_QUANT_TAB_B = (30 | 64);   // Table 3-B.2b: high-rate, sblimit = 30
static const uint8_t PLM_AUDIO_QUANT_TAB_C = 8;           // Table 3-B.2c:  low-rate, sblimit =  8
static const uint8_t PLM_AUDIO_QUANT_TAB_D = 12;          // Table 3-B.2d:  low-rate, sblimit = 12

static const uint8_t QUANT_LUT_STEP_2[3][3] = {
	//44.1 kHz,              48 kHz,                32 kHz
	{ PLM_AUDIO_QUANT_TAB_C, PLM_AUDIO_QUANT_TAB_C, PLM_AUDIO_QUANT_TAB_D }, // 32 - 48 kbit/sec/ch
	{ PLM_AUDIO_QUANT_TAB_A, PLM_AUDIO_QUANT_TAB_A, PLM_AUDIO_QUANT_TAB_A }, // 56 - 80 kbit/sec/ch
	{ PLM_AUDIO_QUANT_TAB_B, PLM_AUDIO_QUANT_TAB_A, PLM_AUDIO_QUANT_TAB_B }  // 96+	 kbit/sec/ch
};

// Quantizer lookup, step 3: B2 table, subband -> nbal, row index
// (upper 4 bits: nbal, lower 4 bits: row index)
static const uint8_t PLM_AUDIO_QUANT_LUT_STEP_3[3][32] = {
	// Low-rate table (3-B.2c and 3-B.2d)
	{
		0x44,0x44,
		0x34,0x34,0x34,0x34,0x34,0x34,0x34,0x34,0x34,0x34
	},
	// High-rate table (3-B.2a and 3-B.2b)
	{
		0x43,0x43,0x43,
		0x42,0x42,0x42,0x42,0x42,0x42,0x42,0x42,
		0x31,0x31,0x31,0x31,0x31,0x31,0x31,0x31,0x31,0x31,0x31,0x31,
		0x20,0x20,0x20,0x20,0x20,0x20,0x20
	},
	// MPEG-2 LSR table (B.2 in ISO 13818-3)
	{
		0x45,0x45,0x45,0x45,
		0x34,0x34,0x34,0x34,0x34,0x34,0x34,
		0x24,0x24,0x24,0x24,0x24,0x24,0x24,0x24,0x24,0x24,
		0x24,0x24,0x24,0x24,0x24,0x24,0x24,0x24,0x24
	}
};

// Quantizer lookup, step 4: table row, allocation[] value -> quant table index
static const uint8_t PLM_AUDIO_QUANT_LUT_STEP4[6][16] = {
	{ 0, 1, 2, 17 },
	{ 0, 1, 2,  3, 4, 5, 6, 17 },
	{ 0, 1, 2,  3, 4, 5, 6,  7,  8,  9, 10, 11, 12, 13, 14, 17 },
	{ 0, 1, 3,  5, 6, 7, 8,  9, 10, 11, 12, 13, 14, 15, 16, 17 },
	{ 0, 1, 2,  4, 5, 6, 7,  8,  9, 10, 11, 12, 13, 14, 15, 17 },
	{ 0, 1, 2,  3, 4, 5, 6,  7,  8,  9, 10, 11, 12, 13, 14, 15 }
};

typedef struct plm_quantizer_spec_t {
	unsigned short levels;
	unsigned char group;
	unsigned char bits;
} plm_quantizer_spec_t;

static const plm_quantizer_spec_t PLM_AUDIO_QUANT_TAB[] = {
	{     3, 1,  5 },  //  1
	{     5, 1,  7 },  //  2
	{     7, 0,  3 },  //  3
	{     9, 1, 10 },  //  4
	{    15, 0,  4 },  //  5
	{    31, 0,  5 },  //  6
	{    63, 0,  6 },  //  7
	{   127, 0,  7 },  //  8
	{   255, 0,  8 },  //  9
	{   511, 0,  9 },  // 10
	{  1023, 0, 10 },  // 11
	{  2047, 0, 11 },  // 12
	{  4095, 0, 12 },  // 13
	{  8191, 0, 13 },  // 14
	{ 16383, 0, 14 },  // 15
	{ 32767, 0, 15 },  // 16
	{ 65535, 0, 16 }   // 17
};

typedef struct plm_audio_t {
	double time;
	int samples_decoded;
	int samplerate_index;
	int bitrate_index;
	int version;
	int layer;
	int mode;
	int bound;
	int v_pos;
	int next_frame_data_size;
	int has_header;
	
	plm_buffer_t *buffer;
	int destroy_buffer_when_done;

	const plm_quantizer_spec_t *allocation[2][32];
	uint8_t scale_factor_info[2][32];
	int scale_factor[2][32][3];
	int sample[2][32][3];

	plm_samples_t samples;
	float D[1024];
	float V[2][1024];
	float U[32];
} plm_audio_t;

int plm_audio_find_frame_sync(plm_audio_t *self);
int plm_audio_decode_header(plm_audio_t *self);
void plm_audio_decode_frame(plm_audio_t *self);
const plm_quantizer_spec_t *plm_audio_read_allocation(plm_audio_t *self, int sb, int tab3);
void plm_audio_read_samples(plm_audio_t *self, int ch, int sb, int part); 
void plm_audio_matrix_transform(int s[32][3], int ss, float *d, int dp);

plm_audio_t *plm_audio_create_with_buffer(plm_buffer_t *buffer, int destroy_when_done) {
	plm_audio_t *self = (plm_audio_t *)malloc(sizeof(plm_audio_t));
	memset(self, 0, sizeof(plm_audio_t));

	self->samples.count = PLM_AUDIO_SAMPLES_PER_FRAME;
	self->buffer = buffer;
	self->destroy_buffer_when_done = destroy_when_done;
	self->samplerate_index = 3; // Indicates 0

	memcpy(self->D, PLM_AUDIO_SYNTHESIS_WINDOW, 512 * sizeof(float));
	memcpy(self->D + 512, PLM_AUDIO_SYNTHESIS_WINDOW, 512 * sizeof(float));

	// Attempt to decode first header
	self->next_frame_data_size = plm_audio_decode_header(self);

	return self;
}

void plm_audio_destroy(plm_audio_t *self) {
	if (self->destroy_buffer_when_done) {
		plm_buffer_destroy(self->buffer);
	}
	free(self);
}

int plm_audio_has_header(plm_audio_t *self) {
	if (self->has_header) {
		return TRUE;
	}
	
	self->next_frame_data_size = plm_audio_decode_header(self);
	return self->has_header;
}

int plm_audio_get_samplerate(plm_audio_t *self) {
	return plm_audio_has_header(self)
		? PLM_AUDIO_SAMPLE_RATE[self->samplerate_index]
		: 0;
}

double plm_audio_get_time(plm_audio_t *self) {
	return self->time;
}

void plm_audio_set_time(plm_audio_t *self, double time) {
	self->samples_decoded = time * 
		(double)PLM_AUDIO_SAMPLE_RATE[self->samplerate_index];
	self->time = time;
}

void plm_audio_rewind(plm_audio_t *self) {
	plm_buffer_rewind(self->buffer);
	self->time = 0;
	self->samples_decoded = 0;
	self->next_frame_data_size = 0;
}

int plm_audio_has_ended(plm_audio_t *self) {
	return plm_buffer_has_ended(self->buffer);
}

plm_samples_t *plm_audio_decode(plm_audio_t *self) {
	// Do we have at least enough information to decode the frame header?
	if (!self->next_frame_data_size) {
		if (!plm_buffer_has(self->buffer, 48)) {
			return NULL;
		}
		self->next_frame_data_size = plm_audio_decode_header(self);
	}

	if (
		self->next_frame_data_size == 0 ||
		!plm_buffer_has(self->buffer, self->next_frame_data_size << 3)
	) {
		return NULL;
	}

	plm_audio_decode_frame(self);
	self->next_frame_data_size = 0;
	
	self->samples.time = self->time;

	self->samples_decoded += PLM_AUDIO_SAMPLES_PER_FRAME;
	self->time = (double)self->samples_decoded / 
		(double)PLM_AUDIO_SAMPLE_RATE[self->samplerate_index];
	
	return &self->samples;
}

int plm_audio_find_frame_sync(plm_audio_t *self) {
	size_t i;
	for (i = self->buffer->bit_index >> 3; i < self->buffer->length-1; i++) {
		if (
			self->buffer->bytes[i] == 0xFF &&
			(self->buffer->bytes[i+1] & 0xFE) == 0xFC
		) {
			self->buffer->bit_index = ((i+1) << 3) + 3;
			return TRUE;
		}
	}
	self->buffer->bit_index = (i + 1) << 3;
	return FALSE;
}

int plm_audio_decode_header(plm_audio_t *self) {
	if (!plm_buffer_has(self->buffer, 48)) {
		return 0;
	}

	plm_buffer_skip_bytes(self->buffer, 0x00);
	int sync = plm_buffer_read(self->buffer, 11);


	// Attempt to resync if no syncword was found. This sucks balls. The MP2 
	// stream contains a syncword just before every frame (11 bits set to 1).
	// However, this syncword is not guaranteed to not occur elswhere in the
	// stream. So, if we have to resync, we also have to check if the header 
	// (samplerate, bitrate) differs from the one we had before. This all
	// may still lead to garbage data being decoded :/

	if (sync != PLM_AUDIO_FRAME_SYNC && !plm_audio_find_frame_sync(self)) {
		return 0;
	}

	self->version = plm_buffer_read(self->buffer, 2);
	self->layer = plm_buffer_read(self->buffer, 2);
	int hasCRC = !plm_buffer_read(self->buffer, 1);

	if (
		self->version != PLM_AUDIO_MPEG_1 ||
		self->layer != PLM_AUDIO_LAYER_II
	) {
		return 0;
	}

	int bitrate_index = plm_buffer_read(self->buffer, 4) - 1;
	if (bitrate_index > 13) {
		return 0;
	}

	int samplerate_index = plm_buffer_read(self->buffer, 2);
	if (samplerate_index == 3) {
		return 0;
	}

	int padding = plm_buffer_read(self->buffer, 1);
	plm_buffer_skip(self->buffer, 1); // f_private
	int mode = plm_buffer_read(self->buffer, 2);

	// If we already have a header, make sure the samplerate, bitrate and mode
	// are still the same, otherwise we might have missed sync.
	if (
		self->has_header && (
			self->bitrate_index != bitrate_index ||
			self->samplerate_index != samplerate_index ||
			self->mode != mode
		)
	) {
		return 0;
	}

	self->bitrate_index = bitrate_index;
	self->samplerate_index = samplerate_index;
	self->mode = mode;
	self->has_header = TRUE;

	// Parse the mode_extension, set up the stereo bound
	if (mode == PLM_AUDIO_MODE_JOINT_STEREO) {
		self->bound = (plm_buffer_read(self->buffer, 2) + 1) << 2;
	}
	else {
		plm_buffer_skip(self->buffer, 2);
		self->bound = (mode == PLM_AUDIO_MODE_MONO) ? 0 : 32;
	}

	// Discard the last 4 bits of the header and the CRC value, if present
	plm_buffer_skip(self->buffer, 4);
	if (hasCRC) {
		plm_buffer_skip(self->buffer, 16);
	}

	// Compute frame size, check if we have enough data to decode the whole
	// frame.
	int bitrate = PLM_AUDIO_BIT_RATE[self->bitrate_index];
	int samplerate = PLM_AUDIO_SAMPLE_RATE[self->samplerate_index];
	int frame_size = (144000 * bitrate / samplerate) + padding;
	return frame_size - (hasCRC ? 6 : 4);
}

void plm_audio_decode_frame(plm_audio_t *self) {
	// Prepare the quantizer table lookups
	int tab3 = 0;
	int sblimit = 0;
	
	int tab1 = (self->mode == PLM_AUDIO_MODE_MONO) ? 0 : 1;
	int tab2 = PLM_AUDIO_QUANT_LUT_STEP_1[tab1][self->bitrate_index];
	tab3 = QUANT_LUT_STEP_2[tab2][self->samplerate_index];
	sblimit = tab3 & 63;
	tab3 >>= 6;

	if (self->bound > sblimit) {
		self->bound = sblimit;
	}

	// Read the allocation information
	for (int sb = 0; sb < self->bound; sb++) {
		self->allocation[0][sb] = plm_audio_read_allocation(self, sb, tab3);
		self->allocation[1][sb] = plm_audio_read_allocation(self, sb, tab3);
	}

	for (int sb = self->bound; sb < sblimit; sb++) {
		self->allocation[0][sb] =
			self->allocation[1][sb] =
			plm_audio_read_allocation(self, sb, tab3);
	}

	// Read scale factor selector information
	int channels = (self->mode == PLM_AUDIO_MODE_MONO) ? 1 : 2;
	for (int sb = 0; sb < sblimit; sb++) {
		for (int ch = 0; ch < channels; ch++) {
			if (self->allocation[ch][sb]) {
				self->scale_factor_info[ch][sb] = plm_buffer_read(self->buffer, 2);
			}
		}
		if (self->mode == PLM_AUDIO_MODE_MONO) {
			self->scale_factor_info[1][sb] = self->scale_factor_info[0][sb];
		}
	}

	// Read scale factors
	for (int sb = 0; sb < sblimit; sb++) {
		for (int ch = 0; ch < channels; ch++) {
			if (self->allocation[ch][sb]) {
				int *sf = self->scale_factor[ch][sb];
				switch (self->scale_factor_info[ch][sb]) {
					case 0:
						sf[0] = plm_buffer_read(self->buffer, 6);
						sf[1] = plm_buffer_read(self->buffer, 6);
						sf[2] = plm_buffer_read(self->buffer, 6);
						break;
					case 1:
						sf[0] = 
						sf[1] = plm_buffer_read(self->buffer, 6);
						sf[2] = plm_buffer_read(self->buffer, 6);
						break;
					case 2:
						sf[0] = 
						sf[1] = 
						sf[2] = plm_buffer_read(self->buffer, 6);
						break;
					case 3:
						sf[0] = plm_buffer_read(self->buffer, 6);
						sf[1] = 
						sf[2] = plm_buffer_read(self->buffer, 6);
						break;
				}
			}
		}
		if (self->mode == PLM_AUDIO_MODE_MONO) {
			self->scale_factor[1][sb][0] = self->scale_factor[0][sb][0];
			self->scale_factor[1][sb][1] = self->scale_factor[0][sb][1];
			self->scale_factor[1][sb][2] = self->scale_factor[0][sb][2];
		}
	}

	// Coefficient input and reconstruction
	int out_pos = 0;
	for (int part = 0; part < 3; part++) {
		for (int granule = 0; granule < 4; granule++) {

			// Read the samples
			for (int sb = 0; sb < self->bound; sb++) {
				plm_audio_read_samples(self, 0, sb, part);
				plm_audio_read_samples(self, 1, sb, part);
			}
			for (int sb = self->bound; sb < sblimit; sb++) {
				plm_audio_read_samples(self, 0, sb, part);
				self->sample[1][sb][0] = self->sample[0][sb][0];
				self->sample[1][sb][1] = self->sample[0][sb][1];
				self->sample[1][sb][2] = self->sample[0][sb][2];
			}
			for (int sb = sblimit; sb < 32; sb++) {
				self->sample[0][sb][0] = 0;
				self->sample[0][sb][1] = 0;
				self->sample[0][sb][2] = 0;
				self->sample[1][sb][0] = 0;
				self->sample[1][sb][1] = 0;
				self->sample[1][sb][2] = 0;
			}

			// Synthesis loop
			for (int p = 0; p < 3; p++) {
				// Shifting step
				self->v_pos = (self->v_pos - 64) & 1023;

				for (int ch = 0; ch < 2; ch++) {
					plm_audio_matrix_transform(self->sample[ch], p, self->V[ch], self->v_pos);

					// Build U, windowing, calculate output
					memset(self->U, 0, sizeof(self->U));

					int d_index = 512 - (self->v_pos >> 1);
					int v_index = (self->v_pos % 128) >> 1;
					while (v_index < 1024) {
						for (int i = 0; i < 32; ++i) {
							self->U[i] += self->D[d_index++] * self->V[ch][v_index++];
						}

						v_index += 128 - 32;
						d_index += 64 - 32;
					}

					d_index -= (512 - 32);
					v_index = (128 - 32 + 1024) - v_index;
					while (v_index < 1024) {
						for (int i = 0; i < 32; ++i) {
							self->U[i] += self->D[d_index++] * self->V[ch][v_index++];
						}

						v_index += 128 - 32;
						d_index += 64 - 32;
					}

					// Output samples
					#ifdef PLM_AUDIO_SEPARATE_CHANNELS
						float *out_channel = ch == 0
							? self->samples.left
							: self->samples.right;
						for (int j = 0; j < 32; j++) {
							out_channel[out_pos + j] = self->U[j] / 2147418112.0f;
						}
					#else
						for (int j = 0; j < 32; j++) {
							self->samples.interleaved[((out_pos + j) << 1) + ch] = 
								self->U[j] / 2147418112.0f;
						}
					#endif
				} // End of synthesis channel loop
				out_pos += 32;
			} // End of synthesis sub-block loop

		} // Decoding of the granule finished
	}

	plm_buffer_align(self->buffer);
}

const plm_quantizer_spec_t *plm_audio_read_allocation(plm_audio_t *self, int sb, int tab3) {
	int tab4 = PLM_AUDIO_QUANT_LUT_STEP_3[tab3][sb];
	int qtab = PLM_AUDIO_QUANT_LUT_STEP4[tab4 & 15][plm_buffer_read(self->buffer, tab4 >> 4)];
	return qtab ? (&PLM_AUDIO_QUANT_TAB[qtab - 1]) : 0;
}

void plm_audio_read_samples(plm_audio_t *self, int ch, int sb, int part) {
	const plm_quantizer_spec_t *q = self->allocation[ch][sb];
	int sf = self->scale_factor[ch][sb][part];
	int *sample = self->sample[ch][sb];
	int val = 0;

	if (!q) {
		// No bits allocated for this subband
		sample[0] = sample[1] = sample[2] = 0;
		return;
	}

	// Resolve scalefactor
	if (sf == 63) {
		sf = 0;
	}
	else {
		int shift = (sf / 3) | 0;
		sf = (PLM_AUDIO_SCALEFACTOR_BASE[sf % 3] + ((1 << shift) >> 1)) >> shift;
	}

	// Decode samples
	int adj = q->levels;
	if (q->group) {
		// Decode grouped samples
		val = plm_buffer_read(self->buffer, q->bits);
		sample[0] = val % adj;
		val /= adj;
		sample[1] = val % adj;
		sample[2] = val / adj;
	}
	else {
		// Decode direct samples
		sample[0] = plm_buffer_read(self->buffer, q->bits);
		sample[1] = plm_buffer_read(self->buffer, q->bits);
		sample[2] = plm_buffer_read(self->buffer, q->bits);
	}

	// Postmultiply samples
	int scale = 65536 / (adj + 1);
	adj = ((adj + 1) >> 1) - 1;

	val = (adj - sample[0]) * scale;
	sample[0] = (val * (sf >> 12) + ((val * (sf & 4095) + 2048) >> 12)) >> 12;

	val = (adj - sample[1]) * scale;
	sample[1] = (val * (sf >> 12) + ((val * (sf & 4095) + 2048) >> 12)) >> 12;

	val = (adj - sample[2]) * scale;
	sample[2] = (val * (sf >> 12) + ((val * (sf & 4095) + 2048) >> 12)) >> 12;
}

void plm_audio_matrix_transform(int s[32][3], int ss, float *d, int dp) {
	float t01, t02, t03, t04, t05, t06, t07, t08, t09, t10, t11, t12,
		t13, t14, t15, t16, t17, t18, t19, t20, t21, t22, t23, t24,
		t25, t26, t27, t28, t29, t30, t31, t32, t33;

	t01 = (float)(s[0][ss] + s[31][ss]); t02 = (float)(s[0][ss] - s[31][ss]) * 0.500602998235f;
	t03 = (float)(s[1][ss] + s[30][ss]); t04 = (float)(s[1][ss] - s[30][ss]) * 0.505470959898f;
	t05 = (float)(s[2][ss] + s[29][ss]); t06 = (float)(s[2][ss] - s[29][ss]) * 0.515447309923f;
	t07 = (float)(s[3][ss] + s[28][ss]); t08 = (float)(s[3][ss] - s[28][ss]) * 0.53104259109f;
	t09 = (float)(s[4][ss] + s[27][ss]); t10 = (float)(s[4][ss] - s[27][ss]) * 0.553103896034f;
	t11 = (float)(s[5][ss] + s[26][ss]); t12 = (float)(s[5][ss] - s[26][ss]) * 0.582934968206f;
	t13 = (float)(s[6][ss] + s[25][ss]); t14 = (float)(s[6][ss] - s[25][ss]) * 0.622504123036f;
	t15 = (float)(s[7][ss] + s[24][ss]); t16 = (float)(s[7][ss] - s[24][ss]) * 0.674808341455f;
	t17 = (float)(s[8][ss] + s[23][ss]); t18 = (float)(s[8][ss] - s[23][ss]) * 0.744536271002f;
	t19 = (float)(s[9][ss] + s[22][ss]); t20 = (float)(s[9][ss] - s[22][ss]) * 0.839349645416f;
	t21 = (float)(s[10][ss] + s[21][ss]); t22 = (float)(s[10][ss] - s[21][ss]) * 0.972568237862f;
	t23 = (float)(s[11][ss] + s[20][ss]); t24 = (float)(s[11][ss] - s[20][ss]) * 1.16943993343f;
	t25 = (float)(s[12][ss] + s[19][ss]); t26 = (float)(s[12][ss] - s[19][ss]) * 1.48416461631f;
	t27 = (float)(s[13][ss] + s[18][ss]); t28 = (float)(s[13][ss] - s[18][ss]) * 2.05778100995f;
	t29 = (float)(s[14][ss] + s[17][ss]); t30 = (float)(s[14][ss] - s[17][ss]) * 3.40760841847f;
	t31 = (float)(s[15][ss] + s[16][ss]); t32 = (float)(s[15][ss] - s[16][ss]) * 10.1900081235f;

	t33 = t01 + t31; t31 = (t01 - t31) * 0.502419286188f;
	t01 = t03 + t29; t29 = (t03 - t29) * 0.52249861494f;
	t03 = t05 + t27; t27 = (t05 - t27) * 0.566944034816f;
	t05 = t07 + t25; t25 = (t07 - t25) * 0.64682178336f;
	t07 = t09 + t23; t23 = (t09 - t23) * 0.788154623451f;
	t09 = t11 + t21; t21 = (t11 - t21) * 1.06067768599f;
	t11 = t13 + t19; t19 = (t13 - t19) * 1.72244709824f;
	t13 = t15 + t17; t17 = (t15 - t17) * 5.10114861869f;
	t15 = t33 + t13; t13 = (t33 - t13) * 0.509795579104f;
	t33 = t01 + t11; t01 = (t01 - t11) * 0.601344886935f;
	t11 = t03 + t09; t09 = (t03 - t09) * 0.899976223136f;
	t03 = t05 + t07; t07 = (t05 - t07) * 2.56291544774f;
	t05 = t15 + t03; t15 = (t15 - t03) * 0.541196100146f;
	t03 = t33 + t11; t11 = (t33 - t11) * 1.30656296488f;
	t33 = t05 + t03; t05 = (t05 - t03) * 0.707106781187f;
	t03 = t15 + t11; t15 = (t15 - t11) * 0.707106781187f;
	t03 += t15;
	t11 = t13 + t07; t13 = (t13 - t07) * 0.541196100146f;
	t07 = t01 + t09; t09 = (t01 - t09) * 1.30656296488f;
	t01 = t11 + t07; t07 = (t11 - t07) * 0.707106781187f;
	t11 = t13 + t09; t13 = (t13 - t09) * 0.707106781187f;
	t11 += t13; t01 += t11;
	t11 += t07; t07 += t13;
	t09 = t31 + t17; t31 = (t31 - t17) * 0.509795579104f;
	t17 = t29 + t19; t29 = (t29 - t19) * 0.601344886935f;
	t19 = t27 + t21; t21 = (t27 - t21) * 0.899976223136f;
	t27 = t25 + t23; t23 = (t25 - t23) * 2.56291544774f;
	t25 = t09 + t27; t09 = (t09 - t27) * 0.541196100146f;
	t27 = t17 + t19; t19 = (t17 - t19) * 1.30656296488f;
	t17 = t25 + t27; t27 = (t25 - t27) * 0.707106781187f;
	t25 = t09 + t19; t19 = (t09 - t19) * 0.707106781187f;
	t25 += t19;
	t09 = t31 + t23; t31 = (t31 - t23) * 0.541196100146f;
	t23 = t29 + t21; t21 = (t29 - t21) * 1.30656296488f;
	t29 = t09 + t23; t23 = (t09 - t23) * 0.707106781187f;
	t09 = t31 + t21; t31 = (t31 - t21) * 0.707106781187f;
	t09 += t31;	t29 += t09;	t09 += t23;	t23 += t31;
	t17 += t29;	t29 += t25;	t25 += t09;	t09 += t27;
	t27 += t23;	t23 += t19; t19 += t31;
	t21 = t02 + t32; t02 = (t02 - t32) * 0.502419286188f;
	t32 = t04 + t30; t04 = (t04 - t30) * 0.52249861494f;
	t30 = t06 + t28; t28 = (t06 - t28) * 0.566944034816f;
	t06 = t08 + t26; t08 = (t08 - t26) * 0.64682178336f;
	t26 = t10 + t24; t10 = (t10 - t24) * 0.788154623451f;
	t24 = t12 + t22; t22 = (t12 - t22) * 1.06067768599f;
	t12 = t14 + t20; t20 = (t14 - t20) * 1.72244709824f;
	t14 = t16 + t18; t16 = (t16 - t18) * 5.10114861869f;
	t18 = t21 + t14; t14 = (t21 - t14) * 0.509795579104f;
	t21 = t32 + t12; t32 = (t32 - t12) * 0.601344886935f;
	t12 = t30 + t24; t24 = (t30 - t24) * 0.899976223136f;
	t30 = t06 + t26; t26 = (t06 - t26) * 2.56291544774f;
	t06 = t18 + t30; t18 = (t18 - t30) * 0.541196100146f;
	t30 = t21 + t12; t12 = (t21 - t12) * 1.30656296488f;
	t21 = t06 + t30; t30 = (t06 - t30) * 0.707106781187f;
	t06 = t18 + t12; t12 = (t18 - t12) * 0.707106781187f;
	t06 += t12;
	t18 = t14 + t26; t26 = (t14 - t26) * 0.541196100146f;
	t14 = t32 + t24; t24 = (t32 - t24) * 1.30656296488f;
	t32 = t18 + t14; t14 = (t18 - t14) * 0.707106781187f;
	t18 = t26 + t24; t24 = (t26 - t24) * 0.707106781187f;
	t18 += t24; t32 += t18;
	t18 += t14; t26 = t14 + t24;
	t14 = t02 + t16; t02 = (t02 - t16) * 0.509795579104f;
	t16 = t04 + t20; t04 = (t04 - t20) * 0.601344886935f;
	t20 = t28 + t22; t22 = (t28 - t22) * 0.899976223136f;
	t28 = t08 + t10; t10 = (t08 - t10) * 2.56291544774f;
	t08 = t14 + t28; t14 = (t14 - t28) * 0.541196100146f;
	t28 = t16 + t20; t20 = (t16 - t20) * 1.30656296488f;
	t16 = t08 + t28; t28 = (t08 - t28) * 0.707106781187f;
	t08 = t14 + t20; t20 = (t14 - t20) * 0.707106781187f;
	t08 += t20;
	t14 = t02 + t10; t02 = (t02 - t10) * 0.541196100146f;
	t10 = t04 + t22; t22 = (t04 - t22) * 1.30656296488f;
	t04 = t14 + t10; t10 = (t14 - t10) * 0.707106781187f;
	t14 = t02 + t22; t02 = (t02 - t22) * 0.707106781187f;
	t14 += t02;	t04 += t14;	t14 += t10;	t10 += t02;
	t16 += t04;	t04 += t08;	t08 += t14;	t14 += t28;
	t28 += t10;	t10 += t20;	t20 += t02;	t21 += t16;
	t16 += t32;	t32 += t04;	t04 += t06;	t06 += t08;
	t08 += t18;	t18 += t14;	t14 += t30;	t30 += t28;
	t28 += t26;	t26 += t10;	t10 += t12;	t12 += t20;
	t20 += t24;	t24 += t02;

	d[dp + 48] = -t33;
	d[dp + 49] = d[dp + 47] = -t21;
	d[dp + 50] = d[dp + 46] = -t17;
	d[dp + 51] = d[dp + 45] = -t16;
	d[dp + 52] = d[dp + 44] = -t01;
	d[dp + 53] = d[dp + 43] = -t32;
	d[dp + 54] = d[dp + 42] = -t29;
	d[dp + 55] = d[dp + 41] = -t04;
	d[dp + 56] = d[dp + 40] = -t03;
	d[dp + 57] = d[dp + 39] = -t06;
	d[dp + 58] = d[dp + 38] = -t25;
	d[dp + 59] = d[dp + 37] = -t08;
	d[dp + 60] = d[dp + 36] = -t11;
	d[dp + 61] = d[dp + 35] = -t18;
	d[dp + 62] = d[dp + 34] = -t09;
	d[dp + 63] = d[dp + 33] = -t14;
	d[dp + 32] = -t05;
	d[dp + 0] = t05; d[dp + 31] = -t30;
	d[dp + 1] = t30; d[dp + 30] = -t27;
	d[dp + 2] = t27; d[dp + 29] = -t28;
	d[dp + 3] = t28; d[dp + 28] = -t07;
	d[dp + 4] = t07; d[dp + 27] = -t26;
	d[dp + 5] = t26; d[dp + 26] = -t23;
	d[dp + 6] = t23; d[dp + 25] = -t10;
	d[dp + 7] = t10; d[dp + 24] = -t15;
	d[dp + 8] = t15; d[dp + 23] = -t12;
	d[dp + 9] = t12; d[dp + 22] = -t19;
	d[dp + 10] = t19; d[dp + 21] = -t20;
	d[dp + 11] = t20; d[dp + 20] = -t13;
	d[dp + 12] = t13; d[dp + 19] = -t24;
	d[dp + 13] = t24; d[dp + 18] = -t31;
	d[dp + 14] = t31; d[dp + 17] = -t02;
	d[dp + 15] = t02; d[dp + 16] = 0.0;
}


#endif // PL_MPEG_IMPLEMENTATION
