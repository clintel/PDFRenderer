/*
 * Copyright 2008 Pirion Systems Pty Ltd, 139 Warry St,
 * Fortitude Valley, Queensland, Australia
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.sun.pdfrenderer.tools.charset;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * <p>
 * Parses text from the PDF reference describing the PDFDocEncoding
 * and verifies it against standard Unicode character names and
 * a few other heuristics to establish correctness. Outputs
 * a table to be used for decoding, destined for PDFStringUtil.
 * </p>
 * 
 * @author Luke Kirby
 */
public class PDFDocEncodingMapGenerator {
    
    public final static void main(String[] args)
            throws IOException {

        final Map<String, Character> unicodeNames = readUnicodeNames();
        // the unicode character for each value in PDFDocEncoding
        final int[] unicodeCharacters = new int[256];
        Set<Integer> mappedCharacters = new HashSet<Integer>();
        List<Integer> problemMappings = new ArrayList<Integer>();

        // Look at PDFDocEncodingMap.txt for a description of its
        // contents
        final BufferedReader r = new BufferedReader(
                new InputStreamReader(
                        PDFDocEncodingMapGenerator.class.
                                getResourceAsStream("PDFDocEncodingMap.txt")));

        // the line representing the character value
        String charLine = null;
        // the line representing the decimal value of the character in
        // the encoding (0-255)
        String decLine = null;
        // the hex representation of the value in the encoding
        String hexLine = null;
        // the octal representation of the value in the encoding
        String octalLine = null;
        // the unicode value of the character with the indicated value in
        // the encoding, expressed as U+XXXX, or "Undefined"
        String unicodeLine = null;
        // the unicode name of the character; not present for all characters
        String nameLine = null;
        // notes for the character; not present for all characters
        String notesLine = null;
        for (int i = 0; i < 256; ++i) {
            // read the charLine, if it wasn't read on the previous iteration
            if (charLine == null) {
                charLine = readEncodingLine(r);
            }
            // read the decimal line if it wasn't read on the previous iteration
            if (decLine == null) {
                decLine = readEncodingLine(r);
            }
            hexLine = readEncodingLine(r);
            octalLine = readEncodingLine(r);
            unicodeLine = readEncodingLine(r);
            nameLine = readEncodingLine(r);
            notesLine = readEncodingLine(r);
            // store the charLine and decLine associated with this iteration;
            // nameLine and notesLine may actually belong to the next
            // iteration, which we will reflect by setting charLine and decLine
            // appropriately
            String ourCharLine = charLine;
            String ourDecLine = decLine;
            if (notesLine != null && parseDecLine(notesLine) == (i + 1)) {
                // what we read as notesLine appears to be the decLine for the
                // iteration. This means that the nameLine we read is actually
                // the charLine for the next iteration.
                charLine = nameLine;
                decLine = notesLine;
                nameLine = null;
                notesLine = null;
            } else {
                // so there was, at least, a nameLine. However, what we
                // read as notesLine might have been the charLine for
                // the next iteration. We'll need to read another line
                // to see if it matches the decLine expected in the next
                // iteration, indicating that this iteration had no notesLine
                final String extraLine = readEncodingLine(r);
                if (extraLine != null && parseDecLine(extraLine) == (i + 1)) {
                    // no notes; what we've read as notes is the next charLine
                    charLine = notesLine;
                    decLine = extraLine;
                    notesLine = null;
                } else {
                    charLine = extraLine;
                    decLine = null;
                }
            }

            System.out.println(ourCharLine + " " +
                    ourDecLine + " " + hexLine + " " + octalLine + " " +
                    unicodeLine + " " + nameLine + " " + notesLine);

            final boolean undefinedCh = "Undefined".equals(unicodeLine);
            final int unicodeCh;
            if (!undefinedCh) {
                unicodeCh = Integer.parseInt(unicodeLine.substring(2), 16);
                if (mappedCharacters.contains(unicodeCh)) {
                    problemMappings.add(i);
                    System.out.println(
                            " !!! " + unicodeCh + " is already mapped to");
                }
                mappedCharacters.add(unicodeCh);
            } else {
                // conventional unmarked character
                unicodeCh = '\uFFFD';
            }

            if (nameLine != null) {
                if (!undefinedCh) {
                    // check that the offered unicode name matches
                    // the value for that name in the standard unicode
                    // mappings; we did have to apply some small corrections
                    // to some entries to have them match.

                    // the nameLine may have the equivalent HTML entity
                    // in brackets after the unicode name line, so we need
                    // to strip them. There are a few pesky unicode
                    // character names with brackets, though, but they're
                    // all control characters, and thus start with brackets,
                    // so we can easily skip them.
                    String unicodeName = nameLine.startsWith("(") ?
                            nameLine :
                            nameLine.replaceFirst("\\s*\\(.*$", "");
                    final Character mappedCh = unicodeNames.get(unicodeName);
                    if (mappedCh == null || mappedCh.charValue() != unicodeCh) {
                        if (mappedCh != null) {
                            System.out.println(" !!! name maps to " +
                                    mappedCh + " (" + (int)mappedCh + ")");
                            problemMappings.add(i);
                        } else {
                            System.out.println(" !!! unmapped name");
                            problemMappings.add(i);
                        }
                    }
                }
            } else {
                // if it's nameless, we expect that the character is alphanum,
                // and identical to the ASCII/ISO-8859 and the lower-byte
                // of UTF-16BE
                if (!(ourCharLine.charAt(0) == (char)i)) {
                    System.out.println("  !!! Unnamed character is not " +
                            "same as Latin1 encoding");
                    problemMappings.add(i);
                }
            }

            if (unicodeCh >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                // needs to use a surrogate, so can't be expressed as a single
                // character, which is how we construct the table
                System.out.println("  !!! supplementary code point! " +
                        "Cannot generate with existing system");
            }

            unicodeCharacters[i] = unicodeCh;
        }

        if (!problemMappings.isEmpty()) {
            System.out.println(problemMappings.size() +
                    " problems for decimal encodings:");
            for (final Integer problemMapping : problemMappings) {
                System.out.println("  " + problemMapping);
            }
            System.out.println("Did not generate table due to errors");
        } else {
            System.out.println(
                    "\tprivate final static char[] PDF_DOC_ENCODING_MAP = " +
                            "new char[] {");
            for (int i = 0; i < 256; i += 8) {
                System.out.print("\t    ");
                for (int j = i; j < i + 8; ++j) {
                    System.out.print(formatArrayEntry(unicodeCharacters[j]));
                }
                System.out.println(
                        " //" + String.format("%02X-%02X", i, i + 7));
            }
            System.out.println("\t};");
        }

    }

    private static String formatArrayEntry(int charVal) {
        return String.format("0x%04X, ", charVal);
    }

    private static String readEncodingLine(BufferedReader r)
            throws IOException {
        String line;
        do {
            line = r.readLine();
        } while (line != null && line.startsWith("##"));
        return line;

    }

    private static int parseDecLine(String notesLine) {
        try {
            return Integer.parseInt(notesLine);
        } catch (NumberFormatException e) {
            // not actually a dec line!
            return -1;
        }
    }


    private static CharChange c(int val, String name) {
        return new CharChange(name, (char) val);
    }

    private static class CharChange {
        String name;
        char value;

        private CharChange(String name, char value) {
            this.name = name;
            this.value = value;
        }
    }

    private static Map<String,Character> readUnicodeNames()
            throws IOException {

        // read the UnicodeData.txt to make a mapping from character name
        // to unicode character value.
        //
        // UnicodeData.txt is from
        // http://unicode.org/Public/UNIDATA/UnicodeData.txt
        // used as per the Terms of Use: http://www.unicode.org/copyright.html
        final Map<String,Character> names = new HashMap<String,Character>();
        final BufferedReader r = new BufferedReader(
                new InputStreamReader(
                        PDFDocEncodingMapGenerator.class.
                                getResourceAsStream("UnicodeData.txt")));
        String line;
        while ((line = r.readLine()) != null) {
            String[] cols = line.split(";");
            final char c = (char) Integer.parseInt(cols[0], 16);
            String name = cols[1];
            if ("<control>".equals(name)) {
                if (cols.length >= 11) {
                    name = "(" + cols[10] + ")";
                } else {
                    name = "(control-" + c + ")";
                }
            }
            if (names.containsKey(name)) {
                throw new IOException("Already found name " + name);
            }
            names.put(name, c);
        }
        return names;
    }
}
