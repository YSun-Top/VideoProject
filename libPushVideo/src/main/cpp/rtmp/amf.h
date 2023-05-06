#ifndef VIDEOPROJECT_LIBPUSHVIDEO_SRC_MAIN_CPP_RTMP_AMF_H_
#define VIDEOPROJECT_LIBPUSHVIDEO_SRC_MAIN_CPP_RTMP_AMF_H_

#include <stdint.h>

#ifndef TRUE
#define TRUE    1
#define FALSE    0
#endif

#ifdef __cplusplus
extern "C"
{
#endif

typedef enum {
  AMF_NUMBER = 0, AMF_BOOLEAN, AMF_STRING, AMF_OBJECT,
  AMF_MOVIECLIP,        /* reserved, not used */
  AMF_NULL, AMF_UNDEFINED, AMF_REFERENCE, AMF_ECMA_ARRAY, AMF_OBJECT_END,
  AMF_STRICT_ARRAY, AMF_DATE, AMF_LONG_STRING, AMF_UNSUPPORTED,
  AMF_RECORDSET,        /* reserved, not used */
  AMF_XML_DOC, AMF_TYPED_OBJECT,
  AMF_AVMPLUS,        /* switch to AMF3 */
  AMF_INVALID = 0xff
} AMFDataType;

typedef enum {
  AMF3_UNDEFINED = 0, AMF3_NULL, AMF3_FALSE, AMF3_TRUE,
  AMF3_INTEGER, AMF3_DOUBLE, AMF3_STRING, AMF3_XML_DOC, AMF3_DATE,
  AMF3_ARRAY, AMF3_OBJECT, AMF3_XML, AMF3_BYTE_ARRAY
} AMF3DataType;

typedef struct AVal {
  char *av_val;
  int av_len;
} AVal;
#define AVC(str)    {str,sizeof(str)-1}
#define AVMATCH(a1, a2)    ((a1)->av_len == (a2)->av_len && !memcmp((a1)->av_val,(a2)->av_val,(a1)->av_len))

struct AMFObjectProperty;

typedef struct AMFObject {
  int o_num;
  struct AMFObjectProperty *o_props;
} AMFObject;

typedef struct AMFObjectProperty {
  AVal p_name;
  AMFDataType p_type;
  union {
    double p_number;
    AVal p_aval;
    AMFObject p_object;
  } p_vu;
  int16_t p_UTCoffset;
} AMFObjectProperty;

typedef struct AMF3ClassDef {
  AVal cd_name;
  char cd_externalizable;
  char cd_dynamic;
  int cd_num;
  AVal *cd_props;
} AMF3ClassDef;

char *AMF_EncodeString(char *output, char *outend, const AVal *str);

char *AMF_EncodeInt16(char *output, char *outend, short nVal);

char *AMF_EncodeInt24(char *output, char *outend, int nVal);

char *AMF_EncodeInt32(char *output, char *outend, int nVal);

char *AMF_EncodeBoolean(char *output, char *outend, int bVal);

char *AMF_EncodeNamedString(char *output, char *outend, const AVal *name, const AVal *value);

char *AMF_EncodeNamedNumber(char *output, char *outend, const AVal *name, double dVal);

char *AMF_EncodeNamedBoolean(char *output, char *outend, const AVal *name, int bVal);

unsigned short AMF_DecodeInt16(const char *data);

unsigned int AMF_DecodeInt24(const char *data);

unsigned int AMF_DecodeInt32(const char *data);

void AMF_DecodeString(const char *data, AVal *str);

int AMF_DecodeBoolean(const char *data);

double AMF_DecodeNumber(const char *data);

char *AMF_EncodeNumber(char *output, char *outend, double dVal);

char *AMF_Encode(AMFObject *obj, char *pBuffer, char *pBufEnd);

char *AMFProp_Encode(AMFObjectProperty *prop, char *pBuffer, char *pBufEnd);

int AMF_Decode(AMFObject *obj, const char *pBuffer, int nSize,
               int bDecodeName);

int AMFProp_Decode(AMFObjectProperty *prop, const char *pBuffer,
                   int nSize, int bDecodeName);

int AMF_DecodeArray(AMFObject *obj, const char *pBuffer, int nSize,
                    int nArrayLen, int bDecodeName);

void AMF_DecodeLongString(const char *data, AVal *str);

void AMF_AddProp(AMFObject *obj, const AMFObjectProperty *prop);
AMFObjectProperty *AMF_GetProp(AMFObject * obj, const AVal * name,
                               int nIndex);

void AMF_Dump(AMFObject *obj);
void AMF_Reset(AMFObject * obj);

void AMFProp_SetName(AMFObjectProperty *prop, AVal *name);

int AMF3_Decode(AMFObject *obj, const char *pBuffer, int nSize,
                int bDecodeName);

int AMF3Prop_Decode(AMFObjectProperty *prop, const char *pBuffer,
                    int nSize, int bDecodeName);

AVal *AMF3CD_GetProp(AMF3ClassDef *cd, int idx);

void AMFProp_Dump(AMFObjectProperty *prop);
void AMFProp_Reset(AMFObjectProperty * prop);
double AMFProp_GetNumber(AMFObjectProperty * prop);
void AMFProp_GetString(AMFObjectProperty * prop, AVal * str);
void AMFProp_GetObject(AMFObjectProperty * prop, AMFObject * obj);

#ifdef __cplusplus
}
#endif

#endif //VIDEOPROJECT_LIBPUSHVIDEO_SRC_MAIN_CPP_RTMP_AMF_H_