## Name

![Icon](/res/icons/16x16/app-sample-editor.png) Sample Editor - Audio sample editor

[Open](launch:///bin/SampleEditor)

## Synopsis

```sh
$ SampleEditor [file]
```

## Description

`Sample Editor` is a graphical audio sample editor for editing audio files. It supports WAV and FLAC formats for both loading and saving, with full support for playback and waveform visualization.

## User Interface

### Toolbar

The toolbar provides quick access to common operations:

-   **New** - Create a new empty sample
-   **Open** - Open an audio file (WAV or FLAC)
-   **Save** - Save the current sample
-   **Save As** - Save with a new name (choose WAV or FLAC format)
-   **Select All** - Select the entire sample
-   **Clear Selection** - Remove the current selection
-   **Play** - Play audio (from cursor position or selection)
-   **Stop** - Stop playback
-   **Zoom In** - Zoom in on the waveform
-   **Zoom Out** - Zoom out on the waveform

### Mouse Controls

-   **Click** - Place the cursor at a position for playback
-   **Click and drag** - Select a region of audio

### Main Workspace

The main workspace displays the audio waveform. The waveform shows the amplitude of the audio over time.

### Playback

Use the Play button or press `Space` to play audio:

-   **With selection** - Plays the selected region
-   **With cursor placed** - Plays from the cursor position to the end
-   **No cursor/selection** - Plays the entire sample

Press Stop or `Space` again to stop playback.

### Zoom Controls

Use the zoom buttons or keyboard shortcuts to adjust the view:

-   **Zoom In** - Show more detail of the waveform
-   **Zoom Out** - Show more of the timeline

### File Menu

-   **New** (`Ctrl+N`) - Reset to initial empty state
-   **Open** (`Ctrl+O`) - Open an audio file (WAV or FLAC)
-   **Save** (`Ctrl+S`) - Save the current file
-   **Save As** (`Ctrl+Shift+S`) - Save with a new filename and format (WAV or FLAC)
-   **Quit** (`Ctrl+Q`) - Exit the application

### Edit Menu

-   **Select All** (`Ctrl+A`) - Select entire sample
-   **Clear Selection** - Remove selection

### View Menu

-   **Zoom In** - Increase waveform detail
-   **Zoom Out** - Decrease waveform detail

## Arguments

-   `file`: Optional audio file to open on startup (WAV or FLAC)

## Examples

```sh
$ SampleEditor
$ SampleEditor /home/anon/Music/sample.wav
$ SampleEditor /home/anon/Music/track.flac
```
