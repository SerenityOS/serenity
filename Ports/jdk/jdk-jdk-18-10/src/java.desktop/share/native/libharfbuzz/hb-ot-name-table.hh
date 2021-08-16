/*
 * Copyright © 2011,2012  Google, Inc.
 *
 *  This is part of HarfBuzz, a text shaping library.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE COPYRIGHT HOLDER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * THE COPYRIGHT HOLDER SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE COPYRIGHT HOLDER HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Google Author(s): Behdad Esfahbod
 */

#ifndef HB_OT_NAME_TABLE_HH
#define HB_OT_NAME_TABLE_HH

#include "hb-open-type.hh"
#include "hb-ot-name-language.hh"
#include "hb-aat-layout.hh"


namespace OT {


#define entry_score var.u16[0]
#define entry_index var.u16[1]


/*
 * name -- Naming
 * https://docs.microsoft.com/en-us/typography/opentype/spec/name
 */
#define HB_OT_TAG_name HB_TAG('n','a','m','e')

#define UNSUPPORTED     42

struct NameRecord
{
  hb_language_t language (hb_face_t *face) const
  {
#ifndef HB_NO_OT_NAME_LANGUAGE
    unsigned int p = platformID;
    unsigned int l = languageID;

    if (p == 3)
      return _hb_ot_name_language_for_ms_code (l);

    if (p == 1)
      return _hb_ot_name_language_for_mac_code (l);

#ifndef HB_NO_OT_NAME_LANGUAGE_AAT
    if (p == 0)
      return face->table.ltag->get_language (l);
#endif

#endif
    return HB_LANGUAGE_INVALID;
  }

  uint16_t score () const
  {
    /* Same order as in cmap::find_best_subtable(). */
    unsigned int p = platformID;
    unsigned int e = encodingID;

    /* 32-bit. */
    if (p == 3 && e == 10) return 0;
    if (p == 0 && e ==  6) return 1;
    if (p == 0 && e ==  4) return 2;

    /* 16-bit. */
    if (p == 3 && e ==  1) return 3;
    if (p == 0 && e ==  3) return 4;
    if (p == 0 && e ==  2) return 5;
    if (p == 0 && e ==  1) return 6;
    if (p == 0 && e ==  0) return 7;

    /* Symbol. */
    if (p == 3 && e ==  0) return 8;

    /* We treat all Mac Latin names as ASCII only. */
    if (p == 1 && e ==  0) return 10; /* 10 is magic number :| */

    return UNSUPPORTED;
  }

  NameRecord* copy (hb_serialize_context_t *c, const void *base) const
  {
    TRACE_SERIALIZE (this);
    auto *out = c->embed (this);
    if (unlikely (!out)) return_trace (nullptr);
    out->offset.serialize_copy (c, offset, base, 0, hb_serialize_context_t::Tail, length);
    return_trace (out);
  }

  bool isUnicode () const
  {
    unsigned int p = platformID;
    unsigned int e = encodingID;

    return (p == 0 ||
            (p == 3 && (e == 0 || e == 1 || e == 10)));
  }

  static int cmp (const void *pa, const void *pb)
  {
    const NameRecord *a = (const NameRecord *)pa;
    const NameRecord *b = (const NameRecord *)pb;

    if (a->platformID != b->platformID)
      return a->platformID - b->platformID;

    if (a->encodingID != b->encodingID)
      return a->encodingID - b->encodingID;

    if (a->languageID != b->languageID)
      return a->languageID - b->languageID;

    if (a->nameID != b->nameID)
      return a->nameID - b->nameID;

    if (a->length != b->length)
      return a->length - b->length;

    return 0;
  }

  bool sanitize (hb_sanitize_context_t *c, const void *base) const
  {
    TRACE_SANITIZE (this);
    return_trace (c->check_struct (this) && offset.sanitize (c, base, length));
  }

  HBUINT16      platformID;     /* Platform ID. */
  HBUINT16      encodingID;     /* Platform-specific encoding ID. */
  HBUINT16      languageID;     /* Language ID. */
  HBUINT16      nameID;         /* Name ID. */
  HBUINT16      length;         /* String length (in bytes). */
  NNOffsetTo<UnsizedArrayOf<HBUINT8>>
                offset;         /* String offset from start of storage area (in bytes). */
  public:
  DEFINE_SIZE_STATIC (12);
};

static int
_hb_ot_name_entry_cmp_key (const void *pa, const void *pb)
{
  const hb_ot_name_entry_t *a = (const hb_ot_name_entry_t *) pa;
  const hb_ot_name_entry_t *b = (const hb_ot_name_entry_t *) pb;

  /* Compare by name_id, then language. */

  if (a->name_id != b->name_id)
    return a->name_id - b->name_id;

  if (a->language == b->language) return 0;
  if (!a->language) return -1;
  if (!b->language) return +1;
  return strcmp (hb_language_to_string (a->language),
                 hb_language_to_string (b->language));
}

static int
_hb_ot_name_entry_cmp (const void *pa, const void *pb)
{
  /* Compare by name_id, then language, then score, then index. */

  int v = _hb_ot_name_entry_cmp_key (pa, pb);
  if (v)
    return v;

  const hb_ot_name_entry_t *a = (const hb_ot_name_entry_t *) pa;
  const hb_ot_name_entry_t *b = (const hb_ot_name_entry_t *) pb;

  if (a->entry_score != b->entry_score)
    return a->entry_score - b->entry_score;

  if (a->entry_index != b->entry_index)
    return a->entry_index - b->entry_index;

  return 0;
}

struct name
{
  static constexpr hb_tag_t tableTag = HB_OT_TAG_name;

  unsigned int get_size () const
  { return min_size + count * nameRecordZ.item_size; }

  template <typename Iterator,
            hb_requires (hb_is_source_of (Iterator, const NameRecord &))>
  bool serialize (hb_serialize_context_t *c,
                  Iterator it,
                  const void *src_string_pool)
  {
    TRACE_SERIALIZE (this);

    if (unlikely (!c->extend_min ((*this))))  return_trace (false);

    this->format = 0;
    this->count = it.len ();

    NameRecord *name_records = (NameRecord *) calloc (it.len (), NameRecord::static_size);
    if (unlikely (!name_records)) return_trace (false);

    hb_array_t<NameRecord> records (name_records, it.len ());

    for (const NameRecord& record : it)
    {
      memcpy (name_records, &record, NameRecord::static_size);
      name_records++;
    }

    records.qsort ();

    c->copy_all (records, src_string_pool);
    free (records.arrayZ);

    if (unlikely (c->ran_out_of_room)) return_trace (false);

    this->stringOffset = c->length ();

    return_trace (true);
  }

  bool subset (hb_subset_context_t *c) const
  {
    TRACE_SUBSET (this);

    name *name_prime = c->serializer->start_embed<name> ();
    if (unlikely (!name_prime)) return_trace (false);

    auto it =
    + nameRecordZ.as_array (count)
    | hb_filter (c->plan->name_ids, &NameRecord::nameID)
    | hb_filter (c->plan->name_languages, &NameRecord::languageID)
    | hb_filter ([&] (const NameRecord& namerecord) { return c->plan->name_legacy || namerecord.isUnicode (); })
    ;

    name_prime->serialize (c->serializer, it, hb_addressof (this + stringOffset));
    return_trace (name_prime->count);
  }

  bool sanitize_records (hb_sanitize_context_t *c) const
  {
    TRACE_SANITIZE (this);
    const void *string_pool = (this+stringOffset).arrayZ;
    return_trace (nameRecordZ.sanitize (c, count, string_pool));
  }

  bool sanitize (hb_sanitize_context_t *c) const
  {
    TRACE_SANITIZE (this);
    return_trace (c->check_struct (this) &&
                  likely (format == 0 || format == 1) &&
                  c->check_array (nameRecordZ.arrayZ, count) &&
                  c->check_range (this, stringOffset) &&
                  sanitize_records (c));
  }

  struct accelerator_t
  {
    void init (hb_face_t *face)
    {
      this->table = hb_sanitize_context_t ().reference_table<name> (face);
      assert (this->table.get_length () >= this->table->stringOffset);
      this->pool = (const char *) (const void *) (this->table+this->table->stringOffset);
      this->pool_len = this->table.get_length () - this->table->stringOffset;
      const hb_array_t<const NameRecord> all_names (this->table->nameRecordZ.arrayZ,
                                                    this->table->count);

      this->names.init ();
      this->names.alloc (all_names.length);

      for (unsigned int i = 0; i < all_names.length; i++)
      {
        hb_ot_name_entry_t *entry = this->names.push ();

        entry->name_id = all_names[i].nameID;
        entry->language = all_names[i].language (face);
        entry->entry_score =  all_names[i].score ();
        entry->entry_index = i;
      }

      this->names.qsort (_hb_ot_name_entry_cmp);
      /* Walk and pick best only for each name_id,language pair,
       * while dropping unsupported encodings. */
      unsigned int j = 0;
      for (unsigned int i = 0; i < this->names.length; i++)
      {
        if (this->names[i].entry_score == UNSUPPORTED ||
            this->names[i].language == HB_LANGUAGE_INVALID)
          continue;
        if (i &&
            this->names[i - 1].name_id  == this->names[i].name_id &&
            this->names[i - 1].language == this->names[i].language)
          continue;
        this->names[j++] = this->names[i];
      }
      this->names.resize (j);
    }

    void fini ()
    {
      this->names.fini ();
      this->table.destroy ();
    }

    int get_index (hb_ot_name_id_t  name_id,
                   hb_language_t    language,
                   unsigned int    *width=nullptr) const
    {
      const hb_ot_name_entry_t key = {name_id, {0}, language};
      const hb_ot_name_entry_t *entry = hb_bsearch (key, (const hb_ot_name_entry_t *) this->names,
                                                    this->names.length,
                                                    sizeof (hb_ot_name_entry_t),
                                                    _hb_ot_name_entry_cmp_key);
      if (!entry)
        return -1;

      if (width)
        *width = entry->entry_score < 10 ? 2 : 1;

      return entry->entry_index;
    }

    hb_bytes_t get_name (unsigned int idx) const
    {
      const hb_array_t<const NameRecord> all_names (table->nameRecordZ.arrayZ, table->count);
      const NameRecord &record = all_names[idx];
      const hb_bytes_t string_pool (pool, pool_len);
      return string_pool.sub_array (record.offset, record.length);
    }

    private:
    const char *pool;
    unsigned int pool_len;
    public:
    hb_blob_ptr_t<name> table;
    hb_vector_t<hb_ot_name_entry_t> names;
  };

  /* We only implement format 0 for now. */
  HBUINT16      format;         /* Format selector (=0/1). */
  HBUINT16      count;          /* Number of name records. */
  NNOffsetTo<UnsizedArrayOf<HBUINT8>>
                stringOffset;   /* Offset to start of string storage (from start of table). */
  UnsizedArrayOf<NameRecord>
                nameRecordZ;    /* The name records where count is the number of records. */
  public:
  DEFINE_SIZE_ARRAY (6, nameRecordZ);
};

#undef entry_index
#undef entry_score

struct name_accelerator_t : name::accelerator_t {};

} /* namespace OT */


#endif /* HB_OT_NAME_TABLE_HH */
