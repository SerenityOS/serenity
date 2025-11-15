## Name

![Icon](/res/icons/16x16/app-sample-editor.png) Sample Editor - Audio sample editor

[Open](launch:///bin/SampleEditor)

## Synopsis

```sh
$ SampleEditor [file]
```

## Description

`Sample Editor` is a graphical audio sample editor for editing audio files. It supports WAV and FLAC formats for both loading and saving, with full support for playback, copy/cut/paste operations, and basic audio editing features.

## User Interface

### Toolbar

The toolbar provides quick access to common operations:

- **New** - Create a new empty sample
- **Open** - Open an audio file (WAV or FLAC)
- **Save** - Save the current sample
- **Save As** - Save with a new name (choose WAV or FLAC format)
- **Copy** - Copy the selected audio region to clipboard
- **Cut** - Cut the selected audio region to clipboard
- **Paste** - Paste audio from clipboard at cursor position
- **Select All** - Select the entire sample
- **Clear Selection** - Remove the current selection
- **Play** - Play audio (from cursor position or selection)
- **Stop** - Stop playback
- **Zoom In** - Zoom in on the waveform
- **Zoom Out** - Zoom out on the waveform

### Mouse Controls

- **Click** - Place the cursor at a position for playback or paste operations
- **Click and drag** - Select a region of audio

### Main Workspace

The main workspace displays the audio waveform. The waveform shows the amplitude of the audio over time.

### Playback

Use the Play button or press `Space` to play audio:

- **With selection** - Plays the selected region
- **With cursor placed** - Plays from the cursor position to the end
- **No cursor/selection** - Plays the entire sample

Press Stop or `Space` again to stop playback.

### Editing Operations

#### Copy and Cut

1. Select a region of audio by clicking and dragging
2. Click Copy or Cut to place it on the clipboard
3. Cut will remove the selected audio from the sample

#### Paste

1. Place the cursor by clicking at the desired position
2. Click Paste to insert clipboard content at the cursor

**Note:** Paste operations require matching sample formats (sample rate, channels, and bit depth). The exception is when pasting into an empty sample, where any format is accepted.

### Zoom Controls

Use the zoom buttons or keyboard shortcuts to adjust the view:

- **Zoom In** - Show more detail of the waveform
- **Zoom Out** - Show more of the timeline

### File Menu

- **New** (`Ctrl+N`) - Reset to initial empty state
- **Open** (`Ctrl+O`) - Open an audio file (WAV or FLAC)
- **Save** (`Ctrl+S`) - Save the current file
- **Save As** (`Ctrl+Shift+S`) - Save with a new filename and format (WAV or FLAC)
- **Quit** (`Ctrl+Q`) - Exit the application

### Edit Menu

- **Copy** (`Ctrl+C`) - Copy selected audio
- **Cut** (`Ctrl+X`) - Cut selected audio
- **Paste** (`Ctrl+V`) - Paste audio at cursor
- **Select All** (`Ctrl+A`) - Select entire sample
- **Clear Selection** - Remove selection

### View Menu

- **Zoom In** - Increase waveform detail
- **Zoom Out** - Decrease waveform detail

## Arguments

- `file`: Optional audio file to open on startup (WAV or FLAC)

## Examples

```sh
$ SampleEditor
$ SampleEditor /home/anon/Music/sample.wav
$ SampleEditor /home/anon/Music/track.flac
```
