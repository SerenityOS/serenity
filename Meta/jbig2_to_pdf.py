#!/usr/bin/env python3

"""
Creates a PDF that embeds a jbig2 image. Useful for viewing .jbig2 files in
PDF viewers, since all PDF viewers support .jbig2 but few image viewers do.

Usage :
% Meta/jbig2_to_pdf.py -o foo.pdf path/to/bitmap.jbig2
% open foo.pdf
"""

from dataclasses import dataclass
import argparse
import collections
import struct
import textwrap


PageInformation = 48
EndOfPage = 49
EndOfStripe = 50
EndOfFile = 51


def dedent(b):
    return textwrap.dedent(b.decode('latin1')).encode('latin1')


@dataclass
class SegmentHeader:
    segment_header_size: int
    type: int
    associated_page: int
    bytes: bytes
    data_size: int
    data: bytes


def read_segment_header(data, offset):
    segment_number, = struct.unpack_from('>I', data, offset)
    flags = data[offset + 4]
    segment_page_association_size_is_32_bits = (flags & 0b100_0000) != 0
    type = (flags & 0b11_1111)

    referred_segments_count = data[offset + 5] >> 5
    if referred_segments_count > 4:
        raise Exception('cannot handle more than 4 referred-to segments')

    if segment_number <= 256:
        ref_size = 1
    elif segment_number <= 65536:
        ref_size = 2
    else:
        ref_size = 4
    segment_header_size = 4 + 1 + 1 + ref_size * referred_segments_count
    pre_page_size = segment_header_size

    if segment_page_association_size_is_32_bits:
        page, = struct.unpack_from('>I', data, offset + segment_header_size)
        segment_header_size += 4
    else:
        page = data[offset + segment_header_size]
        segment_header_size += 1

    data_size, = struct.unpack_from('>I', data, offset + segment_header_size)
    if data_size == 0xffff_ffff:
        if type not in [36, 38, 39]:
            raise Exception('unknown segment size only allowed for generic regions')
    segment_header_size += 4

    bytes = data[offset:offset + segment_header_size]
    if page != 0:
        if segment_page_association_size_is_32_bits:
            bytes = bytes[:pre_page_size] + b'\0\0\0\1' + bytes[pre_page_size + 4:]
        else:
            bytes = bytes[:pre_page_size] + b'\1' + bytes[pre_page_size + 1:]
    return SegmentHeader(segment_header_size, type, page, bytes, data_size, None)


def get_data_size(segment_header, data, offset):
    if segment_header.data_size != 0xffff_ffff:
        return segment_header.data_size

    if len(data) - offset < 23:
        raise Exception('not enough data for segment of unknown size')

    is_mmr = data[offset] & 1 != 0
    end_sequence = b'\x00\x00' if is_mmr else b'\xff\xac'
    index = data.index(end_sequence, offset + 19, len(data) - 4)
    return index - offset + len(end_sequence) + 4


def read_segment_headers(data, is_random_access):
    offset = 0

    segment_headers = []
    while offset < len(data):
        segment_header = read_segment_header(data, offset)
        offset += segment_header.segment_header_size

        if not is_random_access:
            data_size = get_data_size(segment_header, data, offset)
            segment_header.data = data[offset:offset + data_size]
            offset += data_size

        segment_headers.append(segment_header)

        if segment_header.type == EndOfFile:
            break

    if is_random_access:
        for segment_header in segment_headers:
            data_size = get_data_size(segment_header, data, offset)
            segment_header.data = data[offset:offset + data_size]
            offset += data_size

    return segment_headers


def reserialize(segment_headers):
    out_data = bytes()
    for segment_header in segment_headers:
        out_data += segment_header.bytes
        out_data += segment_header.data
    return out_data


def get_dimensions(segment_headers):
    for i, segment_header in enumerate(segment_headers):
        if segment_header.type == PageInformation:
            w, h = struct.unpack_from('>II', segment_header.data)
            if h != 0xffff_ffff:
                return w, h
        if segment_header.type == EndOfPage:
            if segment_headers[i - 1].type != EndOfStripe:
                raise Exception('EndOfPage not preceded by EndOfStripe')
            y, = struct.unpack_from('>I', segment_headers[i - 1].data)
            return w, y + 1
    raise Exception('did not find PageInformation')


def main():
    parser = argparse.ArgumentParser(
        epilog=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("image", help="Input image")
    parser.add_argument("-o", "--output", help="Path to output PDF",
                        required=True)
    args = parser.parse_args()

    with open(args.image, 'rb') as f:
        image_data = f.read()

    # strip jbig2 header
    image_data = image_data[8:]
    flags = image_data[0]
    image_data = image_data[1:]
    is_random_access = flags & 1 == 0
    if flags & 2 == 0:
        image_data = image_data[4:]

    segment_headers = read_segment_headers(image_data, is_random_access)

    # "The JBIG2 file header, end-of-page segments, and end-of-file segment are not
    #  used in PDF. These should be removed before the PDF objects described below
    #  are created."
    # [...]
    # "In the image XObject, however, the
    #  segment’s page number should always be 1; that is, when each such segment is
    #  written to the XObject, the value of its segment page association field should be
    #  set to 1."
    # [...]
    # "If the bit stream contains global segments (segments whose segment page asso-
    #  ciation field contains 0), these segments must be placed in a separate PDF
    #  stream, and the filter parameter listed in Table 3.10 should refer to that stream."
    global_segments = [h for h in segment_headers
                       if h.associated_page == 0 and h.type != EndOfFile]

    pages = collections.defaultdict(list)
    for h in segment_headers:
        if h.associated_page != 0:
            pages[h.associated_page].append(h)

    p = 4 if global_segments else 3

    print(f'{len(pages)} pages')
    page_refs = b' '.join([b'%d 0 R' % (p + 3 * i) for i in range(len(pages))])

    global_entry = b''
    if global_segments:
        global_entry = b'\n                /DecodeParms <</JBIG2Globals 3 0 R>>'

    objs = [dedent(b'''\
              1 0 obj
              <<
                /Type /Catalog
                /Pages 2 0 R
              >>
              endobj
              '''),

            dedent(b'''\
              2 0 obj
              <<
                /Type /Pages
                /Kids [%b]
                /Count %d
              >>
              endobj
              ''' % (page_refs, len(pages))),
            ]

    if global_segments:
        global_segment_data = reserialize(global_segments)
        objs += [
            dedent(b'''\
              3 0 obj
              <</Length %d>>
              stream
              ''' % len(global_segment_data)) +
            global_segment_data +
            dedent(b'''
              endstream
              endobj
              '''),
        ]

    for page in pages:
        segment_headers = pages[page]
        width, height = get_dimensions(segment_headers)
        print(f'dims {width}x{height}')

        segment_headers = [h for h in segment_headers if h.type != EndOfPage]
        image_data = reserialize(segment_headers)

        operators = dedent(b'''\
                      %d 0 0 %d 0 0 cm
                      /Im Do''' % (width, height))

        objs += [
            dedent(b'''\
              %d 0 obj
              <<
                /Type /Page
                /Parent 2 0 R
                /MediaBox [0 0 %d %d]
                /Contents %d 0 R
                /Resources <<
                  /XObject <<
                    /Im %d 0 R
                  >>
                >>
              >>
              endobj
              ''' % (p, width, height, p + 1, p + 2)),

            dedent(b'''\
              %d 0 obj
              <</Length %d>>
              stream
              ''' % (p + 1, len(operators))) +
            operators +
            dedent(b'''
              endstream
              endobj
              '''),

            dedent(b'''\
              %d 0 obj
              <<
                /Length %d
                /Type /XObject
                /Subtype /Image
                /Width %d
                /Height %d
                /ColorSpace /DeviceGray
                /Filter /JBIG2Decode%b
                /BitsPerComponent 1
              >>
              stream
              ''' % (p + 2, len(image_data), width, height, global_entry)) +
            image_data +
            dedent(b'''
              endstream
              endobj
              '''),
        ]

        p += 3

    start = dedent(b'''\
              %PDF-1.4
              %\265\266

              ''')

    with open(args.output, 'wb') as f:
        f.write(start)

        offsets = []
        for obj in objs:
            offsets.append(f.tell())
            f.write(obj)
            f.write(b'\n')

        xref_offset = f.tell()
        f.write(b'xref\n')
        f.write(b'0 %d\n' % (len(objs) + 1))
        f.write(b'0000000000 65536 f \n')
        for offset in offsets:
            f.write(b'%010d 00000 n \n' % offset)
        f.write(b'\n')

        f.write(dedent(b'''\
            trailer
            <<
              /Size %d
              /Root 1 0 R
            >>
            startxref
            %d
            %%%%EOF
            ''' % (len(objs) + 1, xref_offset)))


if __name__ == '__main__':
    main()
