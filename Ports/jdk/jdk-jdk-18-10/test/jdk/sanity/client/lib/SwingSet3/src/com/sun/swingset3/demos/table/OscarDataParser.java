/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package com.sun.swingset3.demos.table;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class OscarDataParser extends DefaultHandler {
    private static final String[] CATEGORIES_IN = {
            "actor", "actress", "bestPicture",
            "actorSupporting", "actressSupporting", "artDirection",
            "assistantDirector", "director", "cinematography",
            "costumeDesign", "danceDirection", "docFeature",
            "docShort", "filmEditing", "foreignFilm",
            "makeup", "musicScore", "musicSong",
            "screenplayAdapted", "screenplayOriginal", "shortAnimation",
            "shortLiveAction", "sound", "soundEditing",
            "specialEffects", "visualEffects", "writing",
            "engEffects", "uniqueArtisticPicture"
    };

    private static final String[] CATEGORIES_OUT = {
            "Best Actor", "Best Actress", "Best Picture",
            "Best Supporting Actor", "Best Supporting Actress", "Best Art Direction",
            "Best Assistant Director", "Best Director", "Best Cinematography",
            "Best Costume Design", "Best Dance Direction", "Best Feature Documentary",
            "Best Short Documentary", "Best Film Editing", "Best Foreign Film",
            "Best Makeup", "Best Musical Score", "Best Song",
            "Best Adapted Screenplay", "Best Original Screenplay", "Best Animation Short",
            "Best Live Action Short", "Best Sound", "Best Sound Editing",
            "Best Special Effects", "Best Visual Effects", "Best Engineering Effects",
            "Best Writing", "Most Unique Artistic Picture"
    };


    private String tempVal;

    //to maintain context
    private OscarCandidate tempOscarCandidate;

    private int count = 0;

    public int getCount() {
        return count;
    }

    public void parseDocument(URL oscarURL) {
        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();

        try {
            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            //parse the file and also register this class for call backs
            InputStream is = new BufferedInputStream(oscarURL.openStream());
            sp.parse(is, this);
            System.out.println("done parsing count="+count);
            is.close();

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        tempVal = "";
        for (int i = 0; i < CATEGORIES_IN.length; i++) {
            if (qName.equalsIgnoreCase(CATEGORIES_IN[i])) {
                tempOscarCandidate = new OscarCandidate(CATEGORIES_OUT[i]);
                tempOscarCandidate.setYear(Integer.parseInt(attributes.getValue("year")));
                if (CATEGORIES_IN[i].equals("screenplayOriginal") &&
                     tempOscarCandidate.getYear() == 2007) {
                }
                return;
            }
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("won")) {
            tempOscarCandidate.setWinner(true);
        } else if (qName.equalsIgnoreCase("lost")) {
            tempOscarCandidate.setWinner(false);
        } else if (qName.equalsIgnoreCase("movie")) {
            tempOscarCandidate.setMovieTitle(tempVal);
        } else if (qName.equalsIgnoreCase("person")) {
            tempOscarCandidate.getPersons().add(tempVal);
        } else {
            // find category
            for (String category : CATEGORIES_IN) {
                if (qName.equalsIgnoreCase(category)) {
                    //add it to the list
                    count++;
                    addCandidate(tempOscarCandidate);
                    break;
                }
            }
        }
    }

    @Override
    public void error(SAXParseException ex) throws SAXException {
        TableDemo.logger.log(Level.SEVERE, "error parsing oscar data ", ex);
    }

    @Override
    public void fatalError(SAXParseException ex) throws SAXException {
        TableDemo.logger.log(Level.SEVERE, "fatal error parsing oscar data ", ex);
    }

    @Override
    public void warning(SAXParseException ex) {
        TableDemo.logger.log(Level.WARNING, "warning occurred while parsing oscar data ", ex);
    }

    @Override
    public void endDocument() throws SAXException {
        TableDemo.logger.log(Level.FINER, "parsed to end of oscar data.");
    }

    protected abstract void addCandidate(OscarCandidate candidate);
}

