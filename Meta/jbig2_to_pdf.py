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
import struct
import textwrap


PageInformation = 48
EndOfFile = 51


def dedent(b):
    return textwrap.dedent(b.decode('latin1')).encode('latin1')


@dataclass
class SegmentHeader:
    segment_header_size: int
    type: int
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

    if segment_page_association_size_is_32_bits:
        segment_header_size += 4
    else:
        segment_header_size += 1

    data_size, = struct.unpack_from('>I', data, offset + segment_header_size)
    if data_size == 0xffff_ffff:
        raise Exception('cannot handle indeterminate length')
    segment_header_size += 4

    bytes = data[offset:offset + segment_header_size]
    return SegmentHeader(segment_header_size, type, bytes, data_size, None)


def read_segment_headers(data, is_random_access):
    offset = 0

    segment_headers = []
    while offset < len(data):
        segment_header = read_segment_header(data, offset)
        offset += segment_header.segment_header_size

        if not is_random_access:
            segment_header.data = data[offset:offset + segment_header.data_size]
            offset += segment_header.data_size

        segment_headers.append(segment_header)

        if segment_header.type == EndOfFile:
            break

    if is_random_access:
        for segment_header in segment_headers:
            segment_header.data = data[offset:offset + segment_header.data_size]
            offset += segment_header.data_size

    return segment_headers


def random_access_to_sequential(segment_headers):
    out_data = bytes()
    for segment_header in segment_headers:
        out_data += segment_header.bytes
        out_data += segment_header.data
    return out_data


def get_dimensions(segment_headers):
    for segment_header in segment_headers:
        if segment_header.type != PageInformation:
            continue
        return struct.unpack_from('>II', segment_header.data)
    raise Exception('did not find PageInformation')


def main():
    parser = argparse.ArgumentParser(
        epilog=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("image", help="Input image")
    parser.add_argument("-o", "--output", help="Path to output PDF")
    args = parser.parse_args()

    with open(args.image, 'rb') as f:
        image_data = f.read()

    # strip jbig2 header
    image_data = image_data[8:]
    is_random_access = image_data[0] & 1 == 0
    if image_data[0] & 2 == 0:
        image_data = image_data[4:]
    image_data = image_data[1:]

    segment_headers = read_segment_headers(image_data, is_random_access)

    width, height = get_dimensions(segment_headers)
    print(f'dims {width}x{height}')

    if is_random_access:
        image_data = random_access_to_sequential(segment_headers)

    start = dedent(b'''\
              %PDF-1.4
              %\265\266

              ''')

    operators = dedent(b'''\
                  %d 0 0 %d 0 0 cm
                  /Im Do''' % (width, height))

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
                /Kids [3 0 R]
                /Count 1
              >>
              endobj
              '''),

            dedent(b'''\
              3 0 obj
              <<
                /Type /Page
                /Parent 2 0 R
                /MediaBox [0 0 %d %d]
                /Contents 4 0 R
                /Resources <<
                  /XObject <<
                    /Im 5 0 R
                  >>
                >>
              >>
              endobj
              ''' % (width, height)),

            dedent(b'''\
              4 0 obj
              <</Length %d>>
              stream
              ''' % len(operators)) +
            operators +
            dedent(b'''
              endstream
              endobj
              '''),

            dedent(b'''\
              5 0 obj
              <<
                /Length %d
                /Type /XObject
                /Subtype /Image
                /Width %d
                /Height %d
                /ColorSpace /DeviceGray
                /Filter /JBIG2Decode
                /BitsPerComponent 1
              >>
              stream
              ''' % (len(image_data), width, height)) +
            image_data +
            dedent(b'''
              endstream
              endobj
              '''),
            ]

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
