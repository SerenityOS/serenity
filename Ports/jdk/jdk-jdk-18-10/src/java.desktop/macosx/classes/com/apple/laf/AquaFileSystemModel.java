/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.apple.laf;


import java.awt.ComponentOrientation;
import java.beans.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;

/**
 * NavServices-like implementation of a file Table
 *
 * Some of it came from BasicDirectoryModel
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class AquaFileSystemModel extends AbstractTableModel implements PropertyChangeListener {
    private final JTable fFileList;
    private FilesLoader filesLoader = null;
    private Vector<File> files = null;

    JFileChooser filechooser = null;
    Vector<SortableFile> fileCache = null;
    Object fileCacheLock;

    Vector<File> directories = null;
    int fetchID = 0;

    private final boolean[] fSortAscending = {true, true};
    // private boolean fSortAscending = true;
    private boolean fSortNames = true;
    private final String[] fColumnNames;
    public static final String SORT_BY_CHANGED = "sortByChanged";
    public static final String SORT_ASCENDING_CHANGED = "sortAscendingChanged";

    public AquaFileSystemModel(final JFileChooser filechooser, final JTable filelist, final String[] colNames) {
        fileCacheLock = new Object();
        this.filechooser = filechooser;
        fFileList = filelist;
        fColumnNames = colNames;
        validateFileCache();
        updateSelectionMode();
    }

    void updateSelectionMode() {
        // Save dialog lists can't be multi select, because all we're selecting is the next folder to open
        final boolean b = filechooser.isMultiSelectionEnabled() && filechooser.getDialogType() != JFileChooser.SAVE_DIALOG;
        fFileList.setSelectionMode(b ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
    }

    public void propertyChange(final PropertyChangeEvent e) {
        final String prop = e.getPropertyName();
        if (prop == JFileChooser.DIRECTORY_CHANGED_PROPERTY || prop == JFileChooser.FILE_VIEW_CHANGED_PROPERTY || prop == JFileChooser.FILE_FILTER_CHANGED_PROPERTY || prop == JFileChooser.FILE_HIDING_CHANGED_PROPERTY) {
            invalidateFileCache();
            validateFileCache();
        } else if (prop.equals(JFileChooser.MULTI_SELECTION_ENABLED_CHANGED_PROPERTY)) {
            updateSelectionMode();
        } else if (prop == JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY) {
            invalidateFileCache();
            validateFileCache();
        } else if (prop.equals("componentOrientation")) {
            ComponentOrientation o = (ComponentOrientation) e.getNewValue();
            JFileChooser cc = (JFileChooser) e.getSource();
            if (o != e.getOldValue()) {
                cc.applyComponentOrientation(o);
            }
            fFileList.setComponentOrientation(o);
            fFileList.getParent().getParent().setComponentOrientation(o);

        }
        if (prop == SORT_BY_CHANGED) {// $ Ought to just resort
            fSortNames = (((Integer)e.getNewValue()).intValue() == 0);
            invalidateFileCache();
            validateFileCache();
            fFileList.repaint();
        }
        if (prop == SORT_ASCENDING_CHANGED) {
            final int sortColumn = (fSortNames ? 0 : 1);
            fSortAscending[sortColumn] = ((Boolean)e.getNewValue()).booleanValue();
            invalidateFileCache();
            validateFileCache();
            fFileList.repaint();
        }
    }

    public void invalidateFileCache() {
        files = null;
        directories = null;

        synchronized(fileCacheLock) {
            if (fileCache != null) {
                final int lastRow = fileCache.size();
                fileCache = null;
                fireTableRowsDeleted(0, lastRow);
            }
        }
    }

    public Vector<File> getDirectories() {
        if (directories != null) { return directories; }
        return directories;
    }

    public Vector<File> getFiles() {
        if (files != null) { return files; }
        files = new Vector<File>();
        directories = new Vector<File>();
        directories.addElement(filechooser.getFileSystemView().createFileObject(filechooser.getCurrentDirectory(), ".."));

        synchronized(fileCacheLock) {
            for (int i = 0; i < fileCache.size(); i++) {
                final SortableFile sf = fileCache.elementAt(i);
                final File f = sf.fFile;
                if (filechooser.isTraversable(f)) {
                    directories.addElement(f);
                } else {
                    files.addElement(f);
                }
            }
        }

        return files;
    }

    public void runWhenDone(final Runnable runnable){
         synchronized (fileCacheLock) {
             if (filesLoader != null) {
                 if (filesLoader.loadThread.isAlive()) {
                     filesLoader.queuedTasks.add(runnable);
                     return;
                 }
             }

             SwingUtilities.invokeLater(runnable);
         }
     }

    public void validateFileCache() {
        final File currentDirectory = filechooser.getCurrentDirectory();

        if (currentDirectory == null) {
            invalidateFileCache();
            return;
        }

        if (filesLoader != null) {
            // interrupt
            filesLoader.loadThread.interrupt();
        }

        fetchID++;

        // PENDING(jeff) pick the size more sensibly
        invalidateFileCache();
        synchronized(fileCacheLock) {
            fileCache = new Vector<SortableFile>(50);
        }

        filesLoader = new FilesLoader(currentDirectory, fetchID);
    }

    public int getColumnCount() {
        return 2;
    }

    public String getColumnName(final int col) {
        return fColumnNames[col];
    }

    public Class<? extends Object> getColumnClass(final int col) {
        if (col == 0) return File.class;
        return Date.class;
    }

    public int getRowCount() {
        synchronized(fileCacheLock) {
            if (fileCache != null) {
                return fileCache.size();
            }
            return 0;
        }
    }

    // SAK: Part of fix for 3168263. The fileCache contains
    // SortableFiles, so when finding a file in the list we need to
    // first create a sortable file.
    public boolean contains(final File o) {
        synchronized(fileCacheLock) {
            if (fileCache != null) {
                return fileCache.contains(new SortableFile(o));
            }
            return false;
        }
    }

    public int indexOf(final File o) {
        synchronized(fileCacheLock) {
            if (fileCache != null) {
                final boolean isAscending = fSortNames ? fSortAscending[0] : fSortAscending[1];
                final int row = fileCache.indexOf(new SortableFile(o));
                return isAscending ? row : fileCache.size() - row - 1;
            }
            return 0;
        }
    }

    // AbstractListModel interface
    public Object getElementAt(final int row) {
        return getValueAt(row, 0);
    }

    // AbstractTableModel interface

    public Object getValueAt(int row, final int col) {
        if (row < 0 || col < 0) return null;
        final boolean isAscending = fSortNames ? fSortAscending[0] : fSortAscending[1];
        synchronized(fileCacheLock) {
            if (fileCache != null) {
                if (!isAscending) row = fileCache.size() - row - 1;
                return fileCache.elementAt(row).getValueAt(col);
            }
            return null;
        }
    }

    // PENDING(jeff) - implement
    public void intervalAdded(final ListDataEvent e) {
    }

    // PENDING(jeff) - implement
    public void intervalRemoved(final ListDataEvent e) {
    }

    protected void sort(final Vector<Object> v) {
        if (fSortNames) sSortNames.quickSort(v, 0, v.size() - 1);
        else sSortDates.quickSort(v, 0, v.size() - 1);
    }

    // Liberated from the 1.1 SortDemo
    //
    // This is a generic version of C.A.R Hoare's Quick Sort
    // algorithm. This will handle arrays that are already
    // sorted, and arrays with duplicate keys.<BR>
    //
    // If you think of a one dimensional array as going from
    // the lowest index on the left to the highest index on the right
    // then the parameters to this function are lowest index or
    // left and highest index or right. The first time you call
    // this function it will be with the parameters 0, a.length - 1.
    //
    // @param a an integer array
    // @param lo0 left boundary of array partition
    // @param hi0 right boundary of array partition
    abstract class QuickSort {
        final void quickSort(final Vector<Object> v, final int lo0, final int hi0) {
            int lo = lo0;
            int hi = hi0;
            SortableFile mid;

            if (hi0 > lo0) {
                // Arbitrarily establishing partition element as the midpoint of
                // the array.
                mid = (SortableFile)v.elementAt((lo0 + hi0) / 2);

                // loop through the array until indices cross
                while (lo <= hi) {
                    // find the first element that is greater than or equal to
                    // the partition element starting from the left Index.
                    //
                    // Nasty to have to cast here. Would it be quicker
                    // to copy the vectors into arrays and sort the arrays?
                    while ((lo < hi0) && lt((SortableFile)v.elementAt(lo), mid)) {
                        ++lo;
                    }

                    // find an element that is smaller than or equal to
                    // the partition element starting from the right Index.
                    while ((hi > lo0) && lt(mid, (SortableFile)v.elementAt(hi))) {
                        --hi;
                    }

                    // if the indexes have not crossed, swap
                    if (lo <= hi) {
                        swap(v, lo, hi);
                        ++lo;
                        --hi;
                    }
                }

                // If the right index has not reached the left side of array
                // must now sort the left partition.
                if (lo0 < hi) {
                    quickSort(v, lo0, hi);
                }

                // If the left index has not reached the right side of array
                // must now sort the right partition.
                if (lo < hi0) {
                    quickSort(v, lo, hi0);
                }

            }
        }

        private void swap(final Vector<Object> a, final int i, final int j) {
            final Object T = a.elementAt(i);
            a.setElementAt(a.elementAt(j), i);
            a.setElementAt(T, j);
        }

        protected abstract boolean lt(SortableFile a, SortableFile b);
    }

    class QuickSortNames extends QuickSort {
        protected boolean lt(final SortableFile a, final SortableFile b) {
            final String aLower = a.fName.toLowerCase();
            final String bLower = b.fName.toLowerCase();
            return aLower.compareTo(bLower) < 0;
        }
    }

    class QuickSortDates extends QuickSort {
        protected boolean lt(final SortableFile a, final SortableFile b) {
            return a.fDateValue < b.fDateValue;
        }
    }

    // for speed in sorting, displaying
    class SortableFile /* extends FileView */{
        File fFile;
        String fName;
        long fDateValue;
        Date fDate;

        SortableFile(final File f) {
            fFile = f;
            fName = fFile.getName();
            fDateValue = fFile.lastModified();
            fDate = new Date(fDateValue);
        }

        public Object getValueAt(final int col) {
            if (col == 0) return fFile;
            return fDate;
        }

        public boolean equals(final Object other) {
            final SortableFile otherFile = (SortableFile)other;
            return otherFile.fFile.equals(fFile);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(fFile);
        }
    }

    class FilesLoader implements Runnable {
        Vector<Runnable> queuedTasks = new Vector<>();
        File currentDirectory = null;
        int fid;
        Thread loadThread;

        public FilesLoader(final File currentDirectory, final int fid) {
            this.currentDirectory = currentDirectory;
            this.fid = fid;
            String name = "Aqua L&F File Loading Thread";
            this.loadThread = new Thread(null, this, name, 0, false);
            this.loadThread.start();
        }

        @Override
        public void run() {
            final Vector<DoChangeContents> runnables = new Vector<DoChangeContents>(10);
            final FileSystemView fileSystem = filechooser.getFileSystemView();

            final File[] list = fileSystem.getFiles(currentDirectory, filechooser.isFileHidingEnabled());

            final Vector<Object> acceptsList = new Vector<Object>();

            for (final File element : list) {
                // Return all files to the file chooser. The UI will disable or enable
                // the file name if the current filter approves.
                acceptsList.addElement(new SortableFile(element));
            }

            // Sort based on settings.
            sort(acceptsList);

            // Don't separate directories from files
            Vector<SortableFile> chunk = new Vector<SortableFile>(10);
            final int listSize = acceptsList.size();
            // run through list grabbing file/dirs in chunks of ten
            for (int i = 0; i < listSize;) {
                SortableFile f;
                for (int j = 0; j < 10 && i < listSize; j++, i++) {
                    f = (SortableFile)acceptsList.elementAt(i);
                    chunk.addElement(f);
                }
                final DoChangeContents runnable = new DoChangeContents(chunk, fid);
                runnables.addElement(runnable);
                SwingUtilities.invokeLater(runnable);
                chunk = new Vector<SortableFile>(10);
                if (loadThread.isInterrupted()) {
                    // interrupted, cancel all runnables
                    cancelRunnables(runnables);
                    return;
                }
            }

            synchronized (fileCacheLock) {
                for (final Runnable r : queuedTasks) {
                    SwingUtilities.invokeLater(r);
                }
            }
        }

        public void cancelRunnables(final Vector<DoChangeContents> runnables) {
            for (int i = 0; i < runnables.size(); i++) {
                runnables.elementAt(i).cancel();
            }
        }
    }

    class DoChangeContents implements Runnable {
        private Vector<SortableFile> contentFiles;
        private boolean doFire = true;
        private final Object lock = new Object();
        private final int fid;

        public DoChangeContents(final Vector<SortableFile> files, final int fid) {
            this.contentFiles = files;
            this.fid = fid;
        }

        synchronized void cancel() {
            synchronized(lock) {
                doFire = false;
            }
        }

        public void run() {
            if (fetchID == fid) {
                synchronized(lock) {
                    if (doFire) {
                        synchronized(fileCacheLock) {
                            if (fileCache != null) {
                                for (int i = 0; i < contentFiles.size(); i++) {
                                    fileCache.addElement(contentFiles.elementAt(i));
                                    fireTableRowsInserted(i, i);
                                }
                            }
                        }
                    }
                    contentFiles = null;
                    directories = null;
                }
            }
        }
    }

    final QuickSortNames sSortNames = new QuickSortNames();
    final QuickSortDates sSortDates = new QuickSortDates();
}
