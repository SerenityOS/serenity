#!/usr/bin/env python3

'''

This writes a dictionary file to Base/res/dictionaries/wordnet.bin

The file contains words from the WordNet database, in a format
designed to be loaded by the Dictionary app, to provide fast
searching, and to be small and simple.

Dictionary .bin file format:

[Header] @ 0
u32 num-words
u32 max-word-length (= 16)
u32 index-offset
u32 word-data-index-offset

[Index] @ index-offset
  Fixed-length ASCII strings for all words,
  tightly packed, padded with zeroes.

[Word data index] @ word-data-index-offset
(num-words times):
  u32 word-data-offset

[Word data]
  u8  num-senses
  (num-senses times):
    u8  sense-number
    u32 definition-offset

[Definition data] @ definition-offset
  u8 part-of-speech
  asciz definition

'''

import struct
import sqlite3
import itertools
import subprocess
import collections
import re
import os


def align(f, n):
    while f.tell() % n:
        f.write(b'\0')


def write_at(f, offset, data):
    here = f.tell()
    f.seek(offset)
    f.write(data)
    f.seek(here)


wordnet_license = '''
WordNet Release 3.0

This software and database is being provided to you, the LICENSEE, by
Princeton University under the following license.  By obtaining, using
and/or copying this software and database, you agree that you have
read, understood, and will comply with these terms and conditions.:

Permission to use, copy, modify and distribute this software and
database and its documentation for any purpose and without fee or
royalty is hereby granted, provided that you agree to comply with
the following copyright notice and statements, including the disclaimer,
and that the same appear on ALL copies of the software, database and
documentation, including modifications that you make for internal
use or for distribution.

WordNet 3.0 Copyright 2006 by Princeton University.  All rights reserved.

THIS SOFTWARE AND DATABASE IS PROVIDED "AS IS" AND PRINCETON
UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR
IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, PRINCETON
UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES OF MERCHANT-
ABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT THE USE
OF THE LICENSED SOFTWARE, DATABASE OR DOCUMENTATION WILL NOT
INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR
OTHER RIGHTS.

The name of Princeton University or Princeton may not be used in
advertising or publicity pertaining to distribution of the software
and/or database.  Title to copyright in this software, database and
any associated documentation shall at all times remain with
Princeton University and LICENSEE agrees to preserve same.
'''.lstrip()


# Download the WordNet database (100MB)
url = 'https://pilotfiber.dl.sourceforge.net/project/wnsql/wnsql3/sqlite/3.1/sqlite-31.db.zip'
zip_path = '/tmp/wordnet.zip'
db_path = '/tmp/wordnet/sqlite-31.db'
db_dir = os.path.dirname(db_path)

if not os.path.exists(db_path):
    subprocess.check_call(
        f'curl -o "{zip_path}" "{url}"',
        shell=True
    )

    subprocess.check_call(
        f'unzip -d "{db_dir}" "{zip_path}"',
        shell=True
    )


db = sqlite3.connect(db_path)
cur = db.cursor()

os.makedirs('Base/res/dictionaries', exist_ok=True)

with open('Base/res/dictionaries/wordnet.license', 'wt') as f:
    f.write(wordnet_license)

out = open('Base/res/dictionaries/wordnet.bin', 'wb')

# To keep things simple, we'll throw out any words that aren't
# entirely made of A-Z letters. We'll be using fixed-length records in
# the index, set at 16 lettters, so also throw out anything longer.

simple_word_re = re.compile('^[a-zA-Z]+$')
max_word_length = 16


def should_keep_word(word):
    if not simple_word_re.match(word):
        return False

    if len(word) > max_word_length:
        return False

    return True


# Write the header.
header_num_words_offset = out.tell()
out.write(struct.pack('<I', 0))
out.write(struct.pack('<I', max_word_length))

header_index_offset = out.tell()
out.write(struct.pack('<I', 0))

header_word_data_index_offset = out.tell()
out.write(struct.pack('<I', 0))


# Find lowercase words. Every casedword has a corresponding
# word, but some of these are not real words (have no senses).
lowercase_word_ids = set(
    x[0] for x in
    cur.execute('''
        SELECT DISTINCT senses.wordid
        FROM
            senses
        WHERE senses.casedwordid IS NULL
    ''').fetchall()
)


# Write the index.
align(out, 8)
index_offset = out.tell()
write_at(out, header_index_offset, struct.pack('<I', index_offset))

num_words = 0
wordid_to_index = {}
wordid_to_word = {}
done_lowercase = set()
for wordid, casedwordid, lemma, cased in cur.execute('''
    SELECT words.wordid, casedwords.casedwordid, words.lemma, casedwords.cased
    FROM
        words
        LEFT JOIN casedwords ON casedwords.wordid = words.wordid
    ORDER BY words.wordid
'''):
    items = []
    if wordid in lowercase_word_ids and wordid not in done_lowercase:
        items.append(
            (wordid, None, lemma, None)
        )
        done_lowercase.add(wordid)

    if casedwordid:
        items.append(
            (wordid, casedwordid, lemma, cased)
        )

    for wordid, casedwordid, lemma, cased in items:

        word = cased or lemma

        if not should_keep_word(word):
            continue

        entry = word.encode('utf-8')
        out.write(entry)
        out.write(b'\0' * (max_word_length - len(entry)))

        wordid_to_index[(wordid, casedwordid)] = num_words
        wordid_to_word[(wordid, casedwordid)] = word
        num_words += 1

assert out.tell() == index_offset + max_word_length * num_words

write_at(out, header_num_words_offset, struct.pack('<I', num_words))
print("Words:", num_words)
print("Index:", (num_words * max_word_length) // 1024, 'KiB')


# Leave space for the word data index
align(out, 8)
word_data_index_offset = out.tell()
write_at(out, header_word_data_index_offset, struct.pack('<I', word_data_index_offset))
out.write(b'\0' * 4 * num_words)


# Write word data.
# Each word has a variable number of senses.
# Each set has a sensenum (1, 2, 3, etc) and a pointer
# to a synset (definition).
synsetid_to_reference_locations = collections.defaultdict(list)
print("Writing word data")
align(out, 8)
word_data_offset = out.tell()
for (wordid, casedwordid), senses in itertools.groupby(
    cur.execute('''
        select
            senses.wordid,
            senses.casedwordid,
            senses.sensenum,
            senses.synsetid
        FROM
            senses
            JOIN synsets ON synsets.synsetid = senses.synsetid
        order by senses.wordid, senses.casedwordid, synsets.pos, senses.sensenum
    '''),
    lambda row: (row[0], row[1])
):

    word_index = wordid_to_index.get((wordid, casedwordid))
    if word_index is None:
        continue

    write_at(out, word_data_index_offset + 4 * word_index, struct.pack('<I', out.tell()))

    senses = list(x for x in senses if x[1] == casedwordid)

    # Number of senses
    out.write(struct.pack('<B', len(senses)))

    # Senses
    for _wordid, sense_casedwordid, sensenum, synsetid in senses:

        synsetid_to_reference_locations[synsetid].append(out.tell())
        out.write(struct.pack('<I', 0))

print("Word data:", (out.tell() - word_data_offset) // 1024, 'KiB')
print('Definitions:', len(synsetid_to_reference_locations))

# Write synsets (definitions)
print("Writing definitions")
align(out, 8)
definitions_offset = out.tell()
for synsetid, pos, definition in cur.execute('''
    select synsetid, pos, definition
    from synsets
'''):
    reflocs = synsetid_to_reference_locations[synsetid]
    if not reflocs:
        continue

    offset = out.tell()

    ref_bytes = struct.pack('<I', offset)
    for refloc in reflocs:
        write_at(out, refloc, ref_bytes)

    # POS
    pos_byte = pos.encode('ascii')
    assert len(pos_byte) == 1

    out.write(pos_byte)

    # Definition
    out.write(definition.encode('utf-8'))

    # End of record
    out.write(b'\0')


print("Definition data:", (out.tell() - definitions_offset) // 1024, 'KiB')
