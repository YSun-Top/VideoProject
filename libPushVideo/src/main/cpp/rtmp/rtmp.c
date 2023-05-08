#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include "rtmp_sys.h"
#include "log.h"

#define RTMP_SIG_SIZE 1536

#define RTMP_LARGE_HEADER_SIZE 12
static const int packetSize[] = {12, 8, 4, 1};

int RTMP_ctrlC;

#ifndef _WIN32
static int clk_tck;
#endif

#undef OSS
#ifdef _WIN32
#define OSS	"WIN"
#elif defined(__sun__)
#define OSS	"SOL"
#elif defined(__APPLE__)
#define OSS	"MAC"
#elif defined(__linux__)
#define OSS    "LNX"
#else
#define OSS	"GNU"
#endif
#define DEF_VERSTR    OSS " 10,0,32,18"

#define SAVC(x)    static const AVal av_##x = AVC(#x)

SAVC(app);
SAVC(connect);
SAVC(flashVer);
SAVC(swfUrl);
SAVC(pageUrl);
SAVC(tcUrl);
SAVC(fpad);
SAVC(capabilities);
SAVC(audioCodecs);
SAVC(videoCodecs);
SAVC(videoFunction);
SAVC(objectEncoding);
SAVC(secureToken);
SAVC(secureTokenResponse);
SAVC(type);
SAVC(nonprivate);
SAVC(FCUnpublish);
SAVC(deleteStream);
SAVC(_result);
SAVC(onBWDone);
SAVC(onFCSubscribe);
SAVC(onFCUnsubscribe);
SAVC(_onbwcheck);
SAVC(_onbwdone);
SAVC(_error);
SAVC(close);
SAVC(code);
SAVC(level);
SAVC(onStatus);
SAVC(playlist_ready);
SAVC(createStream);
SAVC(play);

static const AVal av_NetStream_Failed = AVC("NetStream.Failed");
static const AVal av_NetStream_Play_Failed = AVC("NetStream.Play.Failed");
static const AVal av_NetStream_Play_StreamNotFound =
        AVC("NetStream.Play.StreamNotFound");
static const AVal av_NetConnection_Connect_InvalidApp =
        AVC("NetConnection.Connect.InvalidApp");
static const AVal av_NetStream_Play_Start = AVC("NetStream.Play.Start");
static const AVal av_NetStream_Play_Complete = AVC("NetStream.Play.Complete");
static const AVal av_NetStream_Play_Stop = AVC("NetStream.Play.Stop");
static const AVal av_NetStream_Seek_Notify = AVC("NetStream.Seek.Notify");
static const AVal av_NetStream_Pause_Notify = AVC("NetStream.Pause.Notify");
static const AVal av_NetStream_Play_UnpublishNotify =
        AVC("NetStream.Play.UnpublishNotify");
static const AVal av_NetStream_Publish_Start = AVC("NetStream.Publish.Start");

const char RTMPProtocolStringsLower[][7] = {
        "rtmp",
        "rtmpt",
        "rtmpe",
        "rtmpte",
        "rtmps",
        "rtmpts",
        "",
        "",
        "rtmfp"
};

static const char *RTMPT_cmds[] = {
        "open",
        "send",
        "idle",
        "close"
};

enum {OPT_STR = 0, OPT_INT, OPT_BOOL, OPT_CONN};
static const char *optinfo[] = {
        "string", "integer", "boolean", "AMF"};

#define OFF(x)    offsetof(struct RTMP,x)

static struct urlopt {
  AVal name;
  off_t off;
  int otype;
  int omisc;
  char *use;
} options[] = {
        {AVC("socks"), OFF(Link.sockshost), OPT_STR, 0,
                "Use the specified SOCKS proxy"},
        {AVC("app"), OFF(Link.app), OPT_STR, 0,
                "Name of target app on server"},
        {AVC("tcUrl"), OFF(Link.tcUrl), OPT_STR, 0,
                "URL to played stream"},
        {AVC("pageUrl"), OFF(Link.pageUrl), OPT_STR, 0,
                "URL of played media's web page"},
        {AVC("swfUrl"), OFF(Link.swfUrl), OPT_STR, 0,
                "URL to player SWF file"},
        {AVC("flashver"), OFF(Link.flashVer), OPT_STR, 0,
                "Flash version string (default " DEF_VERSTR ")"},
        {AVC("conn"), OFF(Link.extras), OPT_CONN, 0,
                "Append arbitrary AMF data to Connect message"},
        {AVC("playpath"), OFF(Link.playpath), OPT_STR, 0,
                "Path to target media on server"},
        {AVC("playlist"), OFF(Link.lFlags), OPT_BOOL, RTMP_LF_PLST,
                "Set playlist before play command"},
        {AVC("live"), OFF(Link.lFlags), OPT_BOOL, RTMP_LF_LIVE,
                "Stream is live, no seeking possible"},
        {AVC("subscribe"), OFF(Link.subscribepath), OPT_STR, 0,
                "Stream to subscribe to"},
        {AVC("token"), OFF(Link.token), OPT_STR, 0,
                "Key for SecureToken response"},
        {AVC("swfVfy"), OFF(Link.lFlags), OPT_BOOL, RTMP_LF_SWFV,
                "Perform SWF Verification"},
        {AVC("swfAge"), OFF(Link.swfAge), OPT_INT, 0,
                "Number of days to use cached SWF hash"},
        {AVC("start"), OFF(Link.seekTime), OPT_INT, 0,
                "Stream start position in milliseconds"},
        {AVC("stop"), OFF(Link.stopTime), OPT_INT, 0,
                "Stream stop position in milliseconds"},
        {AVC("buffer"), OFF(m_nBufferMS), OPT_INT, 0,
                "Buffer time in milliseconds"},
        {AVC("timeout"), OFF(Link.timeout), OPT_INT, 0,
                "Session timeout in seconds"},
        {{NULL, 0}, 0, 0}
};

static const AVal truth[] = {
        AVC("1"),
        AVC("on"),
        AVC("yes"),
        AVC("true"),
        {0, 0}
};

typedef enum {
  RTMPT_OPEN = 0, RTMPT_SEND, RTMPT_IDLE, RTMPT_CLOSE
} RTMPTCmd;

static int DumpMetaData(AMFObject *obj);

static int HandShake(RTMP *r, int FP9HandShake);

static int SocksNegotiate(RTMP *r);

static int SendConnectPacket(RTMP *r, RTMPPacket *cp);

static int SendDeleteStream(RTMP *r, double dStreamId);

static int SendBytesReceived(RTMP *r);

static int ReadN(RTMP *r, char *buffer, int n);

static int WriteN(RTMP *r, const char *buffer, int n);

static void DecodeTEA(AVal *key, AVal *text);

static int HTTP_Post(RTMP *r, RTMPTCmd cmd, const char *buf, int len);

static int HTTP_read(RTMP *r, int fill);

static int HandleInvoke(RTMP *r, const char *body, unsigned int nBodySize);

static int HandleMetadata(RTMP *r, char *body, unsigned int len);

static void HandleChangeChunkSize(RTMP *r, const RTMPPacket *packet);

static void HandleAudio(RTMP *r, const RTMPPacket *packet);

static void HandleVideo(RTMP *r, const RTMPPacket *packet);

static void HandleCtrl(RTMP *r, const RTMPPacket *packet);

static void HandleServerBW(RTMP *r, const RTMPPacket *packet);

static void HandleClientBW(RTMP *r, const RTMPPacket *packet);

static int SendFCSubscribe(RTMP *r, AVal *subscribepath);

static int SendCheckBW(RTMP *r);


static void RTMP_OptUsage() {
    int i;

    RTMP_Log(RTMP_LOGERROR, "Valid RTMP options are:\n");
    for (i = 0; options[i].name.av_len; i++) {
        RTMP_Log(RTMP_LOGERROR, "%10s %-7s  %s\n", options[i].name.av_val,
                 optinfo[options[i].otype], options[i].use);
    }
}

static int
DecodeInt32LE(const char *data) {
    unsigned char *c = (unsigned char *) data;
    unsigned int val;

    val = (c[3] << 24) | (c[2] << 16) | (c[1] << 8) | c[0];
    return val;
}

static int
EncodeInt32LE(char *output, int nVal) {
    output[0] = nVal;
    nVal >>= 8;
    output[1] = nVal;
    nVal >>= 8;
    output[2] = nVal;
    nVal >>= 8;
    output[3] = nVal;
    return 4;
}

static void
AV_queue(RTMP_METHOD **vals, int *num, AVal *av, int txn) {
    char *tmp;
    if (!(*num & 0x0f))
        *vals = realloc(*vals, (*num + 16) * sizeof(RTMP_METHOD));
    tmp = malloc(av->av_len + 1);
    memcpy(tmp, av->av_val, av->av_len);
    tmp[av->av_len] = '\0';
    (*vals)[*num].num = txn;
    (*vals)[*num].name.av_len = av->av_len;
    (*vals)[(*num)++].name.av_val = tmp;
}

static void
AV_clear(RTMP_METHOD *vals, int num) {
    int i;
    for (i = 0; i < num; i++)
        free(vals[i].name.av_val);
    free(vals);
}

static int
parseAMF(AMFObject *obj, AVal *av, int *depth) {
    AMFObjectProperty prop = {{0, 0}};
    int i;
    char *p, *arg = av->av_val;

    if (arg[1] == ':') {
        p = (char *) arg + 2;
        switch (arg[0]) {
            case 'B':prop.p_type = AMF_BOOLEAN;
                prop.p_vu.p_number = atoi(p);
                break;
            case 'S':prop.p_type = AMF_STRING;
                prop.p_vu.p_aval.av_val = p;
                prop.p_vu.p_aval.av_len = av->av_len - (p - arg);
                break;
            case 'N':prop.p_type = AMF_NUMBER;
                prop.p_vu.p_number = strtod(p, NULL);
                break;
            case 'Z':prop.p_type = AMF_NULL;
                break;
            case 'O':i = atoi(p);
                if (i) {
                    prop.p_type = AMF_OBJECT;
                } else {
                    (*depth)--;
                    return 0;
                }
                break;
            default:return -1;
        }
    } else if (arg[2] == ':' && arg[0] == 'N') {
        p = strchr(arg + 3, ':');
        if (!p || !*depth)
            return -1;
        prop.p_name.av_val = (char *) arg + 3;
        prop.p_name.av_len = p - (arg + 3);

        p++;
        switch (arg[1]) {
            case 'B':prop.p_type = AMF_BOOLEAN;
                prop.p_vu.p_number = atoi(p);
                break;
            case 'S':prop.p_type = AMF_STRING;
                prop.p_vu.p_aval.av_val = p;
                prop.p_vu.p_aval.av_len = av->av_len - (p - arg);
                break;
            case 'N':prop.p_type = AMF_NUMBER;
                prop.p_vu.p_number = strtod(p, NULL);
                break;
            case 'O':prop.p_type = AMF_OBJECT;
                break;
            default:return -1;
        }
    } else
        return -1;

    if (*depth) {
        AMFObject *o2;
        for (i = 0; i < *depth; i++) {
            o2 = &obj->o_props[obj->o_num - 1].p_vu.p_object;
            obj = o2;
        }
    }
    AMF_AddProp(obj, &prop);
    if (prop.p_type == AMF_OBJECT)
        (*depth)++;
    return 0;
}

static int
SendFCUnpublish(RTMP *r) {
    RTMPPacket packet;
    char pbuf[1024], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_FCUnpublish);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;
    enc = AMF_EncodeString(enc, pend, &r->Link.playpath);
    if (!enc)
        return FALSE;

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, FALSE);
}

static int
add_addr_info(struct sockaddr_in *service, AVal *host, int port) {
    char *hostname;
    int ret = TRUE;
    if (host->av_val[host->av_len]) {
        hostname = malloc(host->av_len + 1);
        memcpy(hostname, host->av_val, host->av_len);
        hostname[host->av_len] = '\0';
    } else {
        hostname = host->av_val;
    }

    service->sin_addr.s_addr = inet_addr(hostname);
    if (service->sin_addr.s_addr == INADDR_NONE) {
        struct hostent *hostbyname = gethostbyname(hostname);
        if (hostbyname == NULL || hostbyname->h_addr == NULL) {
            RTMP_Log(RTMP_LOGERROR, "Problem accessing the DNS. (addr: %s)", hostname);
            ret = FALSE;
            goto finish;
        }
        service->sin_addr = *(struct in_addr *) hostbyname->h_addr;
    }

    service->sin_port = htons(port);
    finish:
    if (hostname != host->av_val)
        free(hostname);
    return ret;
}

static void
AV_erase(RTMP_METHOD *vals, int *num, int i, int freeit) {
    if (freeit)
        free(vals[i].name.av_val);
    (*num)--;
    for (; i < *num; i++) {
        vals[i] = vals[i + 1];
    }
    vals[i].name.av_val = NULL;
    vals[i].name.av_len = 0;
    vals[i].num = 0;
}

static int
SendSecureTokenResponse(RTMP *r, AVal *resp) {
    RTMPPacket packet;
    char pbuf[1024], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_secureTokenResponse);
    enc = AMF_EncodeNumber(enc, pend, 0.0);
    *enc++ = AMF_NULL;
    enc = AMF_EncodeString(enc, pend, resp);
    if (!enc)
        return FALSE;

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, FALSE);
}

SAVC(releaseStream);

static int
SendReleaseStream(RTMP *r) {
    RTMPPacket packet;
    char pbuf[1024], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_releaseStream);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;
    enc = AMF_EncodeString(enc, pend, &r->Link.playpath);
    if (!enc)
        return FALSE;

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, FALSE);
}

SAVC(FCPublish);

static int
SendFCPublish(RTMP *r) {
    RTMPPacket packet;
    char pbuf[1024], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_FCPublish);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;
    enc = AMF_EncodeString(enc, pend, &r->Link.playpath);
    if (!enc)
        return FALSE;

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, FALSE);
}

SAVC(publish);
SAVC(live);
SAVC(record);

static int
SendPublish(RTMP *r) {
    RTMPPacket packet;
    char pbuf[1024], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x04;    /* source channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = r->m_stream_id;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_publish);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;
    enc = AMF_EncodeString(enc, pend, &r->Link.playpath);
    if (!enc)
        return FALSE;

    /* FIXME: should we choose live based on Link.lFlags & RTMP_LF_LIVE? */
    enc = AMF_EncodeString(enc, pend, &av_live);
    if (!enc)
        return FALSE;

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, TRUE);
}

SAVC(set_playlist);
SAVC(0);

static int
SendPlaylist(RTMP *r) {
    RTMPPacket packet;
    char pbuf[1024], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x08;    /* we make 8 our stream channel */
    packet.m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = r->m_stream_id;    /*0x01000000; */
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_set_playlist);
    enc = AMF_EncodeNumber(enc, pend, 0);
    *enc++ = AMF_NULL;
    *enc++ = AMF_ECMA_ARRAY;
    *enc++ = 0;
    *enc++ = 0;
    *enc++ = 0;
    *enc++ = AMF_OBJECT;
    enc = AMF_EncodeNamedString(enc, pend, &av_0, &r->Link.playpath);
    if (!enc)
        return FALSE;
    if (enc + 3 >= pend)
        return FALSE;
    *enc++ = 0;
    *enc++ = 0;
    *enc++ = AMF_OBJECT_END;

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, TRUE);
}

SAVC(ping);
SAVC(pong);

static int
SendPong(RTMP *r, double txn) {
    RTMPPacket packet;
    char pbuf[256], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0x16 * r->m_nBWCheckCounter;    /* temp inc value. till we figure it out. */
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_pong);
    enc = AMF_EncodeNumber(enc, pend, txn);
    *enc++ = AMF_NULL;

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, FALSE);
}

/* Like above, but only check if name is a prefix of property */
int
RTMP_FindPrefixProperty(AMFObject *obj, const AVal *name,
                        AMFObjectProperty *p) {
    int n;
    for (n = 0; n < obj->o_num; n++) {
        AMFObjectProperty *prop = AMF_GetProp(obj, NULL, n);

        if (prop->p_name.av_len > name->av_len &&
                !memcmp(prop->p_name.av_val, name->av_val, name->av_len)) {
            *p = *prop;
            return TRUE;
        }

        if (prop->p_type == AMF_OBJECT) {
            if (RTMP_FindPrefixProperty(&prop->p_vu.p_object, name, p))
                return TRUE;
        }
    }
    return FALSE;
}

static int
SendConnectPacket(RTMP *r, RTMPPacket *cp) {
    RTMPPacket packet;
    char pbuf[4096], *pend = pbuf + sizeof(pbuf);
    char *enc;

    if (cp)
        return RTMP_SendPacket(r, cp, TRUE);

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_connect);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_OBJECT;

    enc = AMF_EncodeNamedString(enc, pend, &av_app, &r->Link.app);
    if (!enc)
        return FALSE;
    if (r->Link.protocol & RTMP_FEATURE_WRITE) {
        enc = AMF_EncodeNamedString(enc, pend, &av_type, &av_nonprivate);
        if (!enc)
            return FALSE;
    }
    if (r->Link.flashVer.av_len) {
        enc = AMF_EncodeNamedString(enc, pend, &av_flashVer, &r->Link.flashVer);
        if (!enc)
            return FALSE;
    }
    if (r->Link.swfUrl.av_len) {
        enc = AMF_EncodeNamedString(enc, pend, &av_swfUrl, &r->Link.swfUrl);
        if (!enc)
            return FALSE;
    }
    if (r->Link.tcUrl.av_len) {
        enc = AMF_EncodeNamedString(enc, pend, &av_tcUrl, &r->Link.tcUrl);
        if (!enc)
            return FALSE;
    }
    if (!(r->Link.protocol & RTMP_FEATURE_WRITE)) {
        enc = AMF_EncodeNamedBoolean(enc, pend, &av_fpad, FALSE);
        if (!enc)
            return FALSE;
        enc = AMF_EncodeNamedNumber(enc, pend, &av_capabilities, 15.0);
        if (!enc)
            return FALSE;
        enc = AMF_EncodeNamedNumber(enc, pend, &av_audioCodecs, r->m_fAudioCodecs);
        if (!enc)
            return FALSE;
        enc = AMF_EncodeNamedNumber(enc, pend, &av_videoCodecs, r->m_fVideoCodecs);
        if (!enc)
            return FALSE;
        enc = AMF_EncodeNamedNumber(enc, pend, &av_videoFunction, 1.0);
        if (!enc)
            return FALSE;
        if (r->Link.pageUrl.av_len) {
            enc = AMF_EncodeNamedString(enc, pend, &av_pageUrl, &r->Link.pageUrl);
            if (!enc)
                return FALSE;
        }
    }
    if (r->m_fEncoding != 0.0 || r->m_bSendEncoding) {    /* AMF0, AMF3 not fully supported yet */
        enc = AMF_EncodeNamedNumber(enc, pend, &av_objectEncoding, r->m_fEncoding);
        if (!enc)
            return FALSE;
    }
    if (enc + 3 >= pend)
        return FALSE;
    *enc++ = 0;
    *enc++ = 0;            /* end of object - 0x00 0x00 0x09 */
    *enc++ = AMF_OBJECT_END;

    /* add auth string */
    if (r->Link.auth.av_len) {
        enc = AMF_EncodeBoolean(enc, pend, r->Link.lFlags & RTMP_LF_AUTH);
        if (!enc)
            return FALSE;
        enc = AMF_EncodeString(enc, pend, &r->Link.auth);
        if (!enc)
            return FALSE;
    }
    if (r->Link.extras.o_num) {
        int i;
        for (i = 0; i < r->Link.extras.o_num; i++) {
            enc = AMFProp_Encode(&r->Link.extras.o_props[i], enc, pend);
            if (!enc)
                return FALSE;
        }
    }
    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, TRUE);
}

static int
SendDeleteStream(RTMP *r, double dStreamId) {
    RTMPPacket packet;
    char pbuf[256], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_deleteStream);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;
    enc = AMF_EncodeNumber(enc, pend, dStreamId);

    packet.m_nBodySize = enc - packet.m_body;

    /* no response expected */
    return RTMP_SendPacket(r, &packet, FALSE);
}

static int
SendBytesReceived(RTMP *r) {
    RTMPPacket packet;
    char pbuf[256], *pend = pbuf + sizeof(pbuf);

    packet.m_nChannel = 0x02;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x03;    /* bytes in */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    packet.m_nBodySize = 4;

    AMF_EncodeInt32(packet.m_body, pend, r->m_nBytesIn);    /* hard coded for now */
    r->m_nBytesInSent = r->m_nBytesIn;

    /*RTMP_Log(RTMP_LOGDEBUG, "Send bytes report. 0x%x (%d bytes)", (unsigned int)m_nBytesIn, m_nBytesIn); */
    return RTMP_SendPacket(r, &packet, FALSE);
}

int
RTMPSockBuf_Fill(RTMPSockBuf *sb) {
    int nBytes;

    if (!sb->sb_size)
        sb->sb_start = sb->sb_buf;

    while (1) {
        nBytes = sizeof(sb->sb_buf) - sb->sb_size - (sb->sb_start - sb->sb_buf);
#if defined(CRYPTO) && !defined(NO_SSL)
        if (sb->sb_ssl)
    {
      nBytes = TLS_read(sb->sb_ssl, sb->sb_start + sb->sb_size, nBytes);
    }
      else
#endif
        {
            nBytes = recv(sb->sb_socket, sb->sb_start + sb->sb_size, nBytes, 0);
        }
        if (nBytes != -1) {
            sb->sb_size += nBytes;
        } else {
            int sockerr = GetSockError();
            RTMP_Log(RTMP_LOGDEBUG, "%s, recv returned %d. GetSockError(): %d (%s)",
                     __FUNCTION__, nBytes, sockerr, strerror(sockerr));
            if (sockerr == EINTR && !RTMP_ctrlC)
                continue;

            if (sockerr == EWOULDBLOCK || sockerr == EAGAIN) {
                sb->sb_timedout = TRUE;
                nBytes = 0;
            }
        }
        break;
    }

    return nBytes;
}

int
RTMPSockBuf_Send(RTMPSockBuf *sb, const char *buf, int len) {
    int rc;

#ifdef _DEBUG
    fwrite(buf, 1, len, netstackdump);
#endif

#if defined(CRYPTO) && !defined(NO_SSL)
    if (sb->sb_ssl)
    {
      rc = TLS_write(sb->sb_ssl, buf, len);
    }
  else
#endif
    {
        rc = send(sb->sb_socket, buf, len, 0);
    }
    return rc;
}

int
RTMPSockBuf_Close(RTMPSockBuf *sb) {
#if defined(CRYPTO) && !defined(NO_SSL)
    if (sb->sb_ssl)
    {
      TLS_shutdown(sb->sb_ssl);
      TLS_close(sb->sb_ssl);
      sb->sb_ssl = NULL;
    }
#endif
    return closesocket(sb->sb_socket);
}

static int
HTTP_read(RTMP *r, int fill) {
    char *ptr;
    int hlen;

    if (fill)
        RTMPSockBuf_Fill(&r->m_sb);
    if (r->m_sb.sb_size < 144)
        return -1;
    if (strncmp(r->m_sb.sb_start, "HTTP/1.1 200 ", 13))
        return -1;
    ptr = strstr(r->m_sb.sb_start, "Content-Length:");
    if (!ptr)
        return -1;
    hlen = atoi(ptr + 16);
    ptr = strstr(ptr, "\r\n\r\n");
    if (!ptr)
        return -1;
    ptr += 4;
    r->m_sb.sb_size -= ptr - r->m_sb.sb_start;
    r->m_sb.sb_start = ptr;
    r->m_unackd--;

    if (!r->m_clientID.av_val) {
        r->m_clientID.av_len = hlen;
        r->m_clientID.av_val = malloc(hlen + 1);
        if (!r->m_clientID.av_val)
            return -1;
        r->m_clientID.av_val[0] = '/';
        memcpy(r->m_clientID.av_val + 1, ptr, hlen - 1);
        r->m_clientID.av_val[hlen] = 0;
        r->m_sb.sb_size = 0;
    } else {
        r->m_polling = *ptr++;
        r->m_resplen = hlen - 1;
        r->m_sb.sb_start++;
        r->m_sb.sb_size--;
    }
    return 0;
}

SAVC(_checkbw);

SAVC(onMetaData);
SAVC(duration);
SAVC(video);
SAVC(audio);

static int
HandleMetadata(RTMP *r, char *body, unsigned int len) {
    /* allright we get some info here, so parse it and print it */
    /* also keep duration or filesize to make a nice progress bar */

    AMFObject obj;
    AVal metastring;
    int ret = FALSE;

    int nRes = AMF_Decode(&obj, body, len, FALSE);
    if (nRes < 0) {
        RTMP_Log(RTMP_LOGERROR, "%s, error decoding meta data packet", __FUNCTION__);
        return FALSE;
    }

    AMF_Dump(&obj);
    AMFProp_GetString(AMF_GetProp(&obj, NULL, 0), &metastring);

    if (AVMATCH(&metastring, &av_onMetaData)) {
        AMFObjectProperty prop;
        /* Show metadata */
        RTMP_Log(RTMP_LOGINFO, "Metadata:");
        DumpMetaData(&obj);
        if (RTMP_FindFirstMatchingProperty(&obj, &av_duration, &prop)) {
            r->m_fDuration = prop.p_vu.p_number;
            /*RTMP_Log(RTMP_LOGDEBUG, "Set duration: %.2f", m_fDuration); */
        }
        /* Search for audio or video tags */
        if (RTMP_FindPrefixProperty(&obj, &av_video, &prop))
            r->m_read.dataType |= 1;
        if (RTMP_FindPrefixProperty(&obj, &av_audio, &prop))
            r->m_read.dataType |= 4;
        ret = TRUE;
    }
    AMF_Reset(&obj);
    return ret;
}

static void
HandleChangeChunkSize(RTMP *r, const RTMPPacket *packet) {
    if (packet->m_nBodySize >= 4) {
        r->m_inChunkSize = AMF_DecodeInt32(packet->m_body);
        RTMP_Log(RTMP_LOGDEBUG, "%s, received: chunk size change to %d", __FUNCTION__,
                 r->m_inChunkSize);
    }
}

static void
HandleAudio(RTMP *r, const RTMPPacket *packet) {
}

static void
HandleVideo(RTMP *r, const RTMPPacket *packet) {
}

static void
HandleCtrl(RTMP *r, const RTMPPacket *packet) {
    short nType = -1;
    unsigned int tmp;
    if (packet->m_body && packet->m_nBodySize >= 2)
        nType = AMF_DecodeInt16(packet->m_body);
    RTMP_Log(RTMP_LOGDEBUG, "%s, received ctrl. type: %d, len: %d", __FUNCTION__, nType,
             packet->m_nBodySize);
    /*RTMP_LogHex(packet.m_body, packet.m_nBodySize); */

    if (packet->m_nBodySize >= 6) {
        switch (nType) {
            case 0:tmp = AMF_DecodeInt32(packet->m_body + 2);
                RTMP_Log(RTMP_LOGDEBUG, "%s, Stream Begin %d", __FUNCTION__, tmp);
                break;

            case 1:tmp = AMF_DecodeInt32(packet->m_body + 2);
                RTMP_Log(RTMP_LOGDEBUG, "%s, Stream EOF %d", __FUNCTION__, tmp);
                if (r->m_pausing == 1)
                    r->m_pausing = 2;
                break;

            case 2:tmp = AMF_DecodeInt32(packet->m_body + 2);
                RTMP_Log(RTMP_LOGDEBUG, "%s, Stream Dry %d", __FUNCTION__, tmp);
                break;

            case 4:tmp = AMF_DecodeInt32(packet->m_body + 2);
                RTMP_Log(RTMP_LOGDEBUG, "%s, Stream IsRecorded %d", __FUNCTION__, tmp);
                break;

            case 6:        /* server ping. reply with pong. */
                tmp = AMF_DecodeInt32(packet->m_body + 2);
                RTMP_Log(RTMP_LOGDEBUG, "%s, Ping %d", __FUNCTION__, tmp);
                RTMP_SendCtrl(r, 0x07, tmp, 0);
                break;

                /* FMS 3.5 servers send the following two controls to let the client
                 * know when the server has sent a complete buffer. I.e., when the
                 * server has sent an amount of data equal to m_nBufferMS in duration.
                 * The server meters its output so that data arrives at the client
                 * in realtime and no faster.
                 *
                 * The rtmpdump program tries to set m_nBufferMS as large as
                 * possible, to force the server to send data as fast as possible.
                 * In practice, the server appears to cap this at about 1 hour's
                 * worth of data. After the server has sent a complete buffer, and
                 * sends this BufferEmpty message, it will wait until the play
                 * duration of that buffer has passed before sending a new buffer.
                 * The BufferReady message will be sent when the new buffer starts.
                 * (There is no BufferReady message for the very first buffer;
                 * presumably the Stream Begin message is sufficient for that
                 * purpose.)
                 *
                 * If the network speed is much faster than the data bitrate, then
                 * there may be long delays between the end of one buffer and the
                 * start of the next.
                 *
                 * Since usually the network allows data to be sent at
                 * faster than realtime, and rtmpdump wants to download the data
                 * as fast as possible, we use this RTMP_LF_BUFX hack: when we
                 * get the BufferEmpty message, we send a Pause followed by an
                 * Unpause. This causes the server to send the next buffer immediately
                 * instead of waiting for the full duration to elapse. (That's
                 * also the purpose of the ToggleStream function, which rtmpdump
                 * calls if we get a read timeout.)
                 *
                 * Media player apps don't need this hack since they are just
                 * going to play the data in realtime anyway. It also doesn't work
                 * for live streams since they obviously can only be sent in
                 * realtime. And it's all moot if the network speed is actually
                 * slower than the media bitrate.
                 */
            case 31:tmp = AMF_DecodeInt32(packet->m_body + 2);
                RTMP_Log(RTMP_LOGDEBUG, "%s, Stream BufferEmpty %d", __FUNCTION__, tmp);
                if (!(r->Link.lFlags & RTMP_LF_BUFX))
                    break;
                if (!r->m_pausing) {
                    r->m_pauseStamp = r->m_channelTimestamp[r->m_mediaChannel];
                    RTMP_SendPause(r, TRUE, r->m_pauseStamp);
                    r->m_pausing = 1;
                } else if (r->m_pausing == 2) {
                    RTMP_SendPause(r, FALSE, r->m_pauseStamp);
                    r->m_pausing = 3;
                }
                break;

            case 32:tmp = AMF_DecodeInt32(packet->m_body + 2);
                RTMP_Log(RTMP_LOGDEBUG, "%s, Stream BufferReady %d", __FUNCTION__, tmp);
                break;

            default:tmp = AMF_DecodeInt32(packet->m_body + 2);
                RTMP_Log(RTMP_LOGDEBUG, "%s, Stream xx %d", __FUNCTION__, tmp);
                break;
        }

    }

    if (nType == 0x1A) {
        RTMP_Log(RTMP_LOGDEBUG, "%s, SWFVerification ping received: ", __FUNCTION__);
#ifdef CRYPTO
        /*RTMP_LogHex(packet.m_body, packet.m_nBodySize); */

      /* respond with HMAC SHA256 of decompressed SWF, key is the 30byte player key, also the last 30 bytes of the server handshake are applied */
      if (r->Link.SWFSize)
    {
      RTMP_SendCtrl(r, 0x1B, 0, 0);
    }
      else
    {
      RTMP_Log(RTMP_LOGERROR,
          "%s: Ignoring SWFVerification request, use --swfVfy!",
          __FUNCTION__);
    }
#else
        RTMP_Log(RTMP_LOGERROR,
                 "%s: Ignoring SWFVerification request, no CRYPTO support!",
                 __FUNCTION__);
#endif
    }
}

static void
HandleServerBW(RTMP *r, const RTMPPacket *packet) {
    r->m_nServerBW = AMF_DecodeInt32(packet->m_body);
    RTMP_Log(RTMP_LOGDEBUG, "%s: server BW = %d", __FUNCTION__, r->m_nServerBW);
}

static void
HandleClientBW(RTMP *r, const RTMPPacket *packet) {
    r->m_nClientBW = AMF_DecodeInt32(packet->m_body);
    if (packet->m_nBodySize > 4)
        r->m_nClientBW2 = packet->m_body[4];
    else
        r->m_nClientBW2 = -1;
    RTMP_Log(RTMP_LOGDEBUG, "%s: client BW = %d %d", __FUNCTION__, r->m_nClientBW,
             r->m_nClientBW2);
}

static int
HandleInvoke(RTMP *r, const char *body, unsigned int nBodySize) {
    AMFObject obj;
    AVal method;
    int txn;
    int ret = 0, nRes;
    if (body[0] != 0x02)        /* make sure it is a string method name we start with */
    {
        RTMP_Log(RTMP_LOGWARNING, "%s, Sanity failed. no string method in invoke packet",
                 __FUNCTION__);
        return 0;
    }

    nRes = AMF_Decode(&obj, body, nBodySize, FALSE);
    if (nRes < 0) {
        RTMP_Log(RTMP_LOGERROR, "%s, error decoding invoke packet", __FUNCTION__);
        return 0;
    }

    AMF_Dump(&obj);
    AMFProp_GetString(AMF_GetProp(&obj, NULL, 0), &method);
    txn = (int) AMFProp_GetNumber(AMF_GetProp(&obj, NULL, 1));
    RTMP_Log(RTMP_LOGDEBUG, "%s, server invoking <%s>", __FUNCTION__, method.av_val);

    if (AVMATCH(&method, &av__result)) {
        AVal methodInvoked = {0};
        int i;

        for (i = 0; i < r->m_numCalls; i++) {
            if (r->m_methodCalls[i].num == txn) {
                methodInvoked = r->m_methodCalls[i].name;
                AV_erase(r->m_methodCalls, &r->m_numCalls, i, FALSE);
                break;
            }
        }
        if (!methodInvoked.av_val) {
            RTMP_Log(RTMP_LOGDEBUG, "%s, received result id %d without matching request",
                     __FUNCTION__, txn);
            goto leave;
        }

        RTMP_Log(RTMP_LOGDEBUG, "%s, received result for method call <%s>", __FUNCTION__,
                 methodInvoked.av_val);

        if (AVMATCH(&methodInvoked, &av_connect)) {
            if (r->Link.token.av_len) {
                AMFObjectProperty p;
                if (RTMP_FindFirstMatchingProperty(&obj, &av_secureToken, &p)) {
                    DecodeTEA(&r->Link.token, &p.p_vu.p_aval);
                    SendSecureTokenResponse(r, &p.p_vu.p_aval);
                }
            }
            if (r->Link.protocol & RTMP_FEATURE_WRITE) {
                SendReleaseStream(r);
                SendFCPublish(r);
            } else {
                RTMP_SendServerBW(r);
                RTMP_SendCtrl(r, 3, 0, 300);
            }
            RTMP_SendCreateStream(r);

            if (!(r->Link.protocol & RTMP_FEATURE_WRITE)) {
                /* Send the FCSubscribe if live stream or if subscribepath is set */
                if (r->Link.subscribepath.av_len)
                    SendFCSubscribe(r, &r->Link.subscribepath);
                else if (r->Link.lFlags & RTMP_LF_LIVE)
                    SendFCSubscribe(r, &r->Link.playpath);
            }
        } else if (AVMATCH(&methodInvoked, &av_createStream)) {
            r->m_stream_id = (int) AMFProp_GetNumber(AMF_GetProp(&obj, NULL, 3));

            if (r->Link.protocol & RTMP_FEATURE_WRITE) {
                SendPublish(r);
            } else {
                if (r->Link.lFlags & RTMP_LF_PLST)
                    SendPlaylist(r);
                SendPlay(r);
                RTMP_SendCtrl(r, 3, r->m_stream_id, r->m_nBufferMS);
            }
        } else if (AVMATCH(&methodInvoked, &av_play) ||
                AVMATCH(&methodInvoked, &av_publish)) {
            r->m_bPlaying = TRUE;
        }
        free(methodInvoked.av_val);
    } else if (AVMATCH(&method, &av_onBWDone)) {
        if (!r->m_nBWCheckCounter)
            SendCheckBW(r);
    } else if (AVMATCH(&method, &av_onFCSubscribe)) {
        /* SendOnFCSubscribe(); */
    } else if (AVMATCH(&method, &av_onFCUnsubscribe)) {
        RTMP_Close(r);
        ret = 1;
    } else if (AVMATCH(&method, &av_ping)) {
        SendPong(r, txn);
    } else if (AVMATCH(&method, &av__onbwcheck)) {
        SendCheckBWResult(r, txn);
    } else if (AVMATCH(&method, &av__onbwdone)) {
        int i;
        for (i = 0; i < r->m_numCalls; i++)
            if (AVMATCH(&r->m_methodCalls[i].name, &av__checkbw)) {
                AV_erase(r->m_methodCalls, &r->m_numCalls, i, TRUE);
                break;
            }
    } else if (AVMATCH(&method, &av__error)) {
        RTMP_Log(RTMP_LOGERROR, "rtmp server sent error");
    } else if (AVMATCH(&method, &av_close)) {
        RTMP_Log(RTMP_LOGERROR, "rtmp server requested close");
        RTMP_Close(r);
    } else if (AVMATCH(&method, &av_onStatus)) {
        AMFObject obj2;
        AVal code, level;
        AMFProp_GetObject(AMF_GetProp(&obj, NULL, 3), &obj2);
        AMFProp_GetString(AMF_GetProp(&obj2, &av_code, -1), &code);
        AMFProp_GetString(AMF_GetProp(&obj2, &av_level, -1), &level);

        RTMP_Log(RTMP_LOGDEBUG, "%s, onStatus: %s", __FUNCTION__, code.av_val);
        if (AVMATCH(&code, &av_NetStream_Failed)
                || AVMATCH(&code, &av_NetStream_Play_Failed)
                || AVMATCH(&code, &av_NetStream_Play_StreamNotFound)
                || AVMATCH(&code, &av_NetConnection_Connect_InvalidApp)) {
            r->m_stream_id = -1;
            RTMP_Close(r);
            RTMP_Log(RTMP_LOGERROR, "Closing connection: %s", code.av_val);
        } else if (AVMATCH(&code, &av_NetStream_Play_Start)) {
            int i;
            r->m_bPlaying = TRUE;
            for (i = 0; i < r->m_numCalls; i++) {
                if (AVMATCH(&r->m_methodCalls[i].name, &av_play)) {
                    AV_erase(r->m_methodCalls, &r->m_numCalls, i, TRUE);
                    break;
                }
            }
        } else if (AVMATCH(&code, &av_NetStream_Publish_Start)) {
            int i;
            r->m_bPlaying = TRUE;
            for (i = 0; i < r->m_numCalls; i++) {
                if (AVMATCH(&r->m_methodCalls[i].name, &av_publish)) {
                    AV_erase(r->m_methodCalls, &r->m_numCalls, i, TRUE);
                    break;
                }
            }
        }

            /* Return 1 if this is a Play.Complete or Play.Stop */
        else if (AVMATCH(&code, &av_NetStream_Play_Complete)
                || AVMATCH(&code, &av_NetStream_Play_Stop)
                || AVMATCH(&code, &av_NetStream_Play_UnpublishNotify)) {
            RTMP_Close(r);
            ret = 1;
        } else if (AVMATCH(&code, &av_NetStream_Seek_Notify)) {
            r->m_read.flags &= ~RTMP_READ_SEEKING;
        } else if (AVMATCH(&code, &av_NetStream_Pause_Notify)) {
            if (r->m_pausing == 1 || r->m_pausing == 2) {
                RTMP_SendPause(r, FALSE, r->m_pauseStamp);
                r->m_pausing = 3;
            }
        }
    } else if (AVMATCH(&method, &av_playlist_ready)) {
        int i;
        for (i = 0; i < r->m_numCalls; i++) {
            if (AVMATCH(&r->m_methodCalls[i].name, &av_set_playlist)) {
                AV_erase(r->m_methodCalls, &r->m_numCalls, i, TRUE);
                break;
            }
        }
    } else {

    }
    leave:
    AMF_Reset(&obj);
    return ret;
}

SAVC(FCSubscribe);

static int
SendFCSubscribe(RTMP *r, AVal *subscribepath) {
    RTMPPacket packet;
    char pbuf[512], *pend = pbuf + sizeof(pbuf);
    char *enc;
    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    RTMP_Log(RTMP_LOGDEBUG, "FCSubscribe: %s", subscribepath->av_val);
    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_FCSubscribe);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;
    enc = AMF_EncodeString(enc, pend, subscribepath);

    if (!enc)
        return FALSE;

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, TRUE);
}

static int
SendCheckBW(RTMP *r) {
    RTMPPacket packet;
    char pbuf[256], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;    /* RTMP_GetTime(); */
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av__checkbw);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;

    packet.m_nBodySize = enc - packet.m_body;

    /* triggers _onbwcheck and eventually results in _onbwdone */
    return RTMP_SendPacket(r, &packet, FALSE);
}

static int
ReadN(RTMP *r, char *buffer, int n) {
    int nOriginalSize = n;
    int avail;
    char *ptr;

    r->m_sb.sb_timedout = FALSE;

#ifdef _DEBUG
    memset(buffer, 0, n);
#endif

    ptr = buffer;
    while (n > 0) {
        int nBytes = 0, nRead;
        if (r->Link.protocol & RTMP_FEATURE_HTTP) {
            while (!r->m_resplen) {
                if (r->m_sb.sb_size < 144) {
                    if (!r->m_unackd)
                        HTTP_Post(r, RTMPT_IDLE, "", 1);
                    if (RTMPSockBuf_Fill(&r->m_sb) < 1) {
                        if (!r->m_sb.sb_timedout)
                            RTMP_Close(r);
                        return 0;
                    }
                }
                HTTP_read(r, 0);
            }
            if (r->m_resplen && !r->m_sb.sb_size)
                RTMPSockBuf_Fill(&r->m_sb);
            avail = r->m_sb.sb_size;
            if (avail > r->m_resplen)
                avail = r->m_resplen;
        } else {
            avail = r->m_sb.sb_size;
            if (avail == 0) {
                if (RTMPSockBuf_Fill(&r->m_sb) < 1) {
                    if (!r->m_sb.sb_timedout)
                        RTMP_Close(r);
                    return 0;
                }
                avail = r->m_sb.sb_size;
            }
        }
        nRead = ((n < avail) ? n : avail);
        if (nRead > 0) {
            memcpy(ptr, r->m_sb.sb_start, nRead);
            r->m_sb.sb_start += nRead;
            r->m_sb.sb_size -= nRead;
            nBytes = nRead;
            r->m_nBytesIn += nRead;
            if (r->m_bSendCounter
                    && r->m_nBytesIn > r->m_nBytesInSent + r->m_nClientBW / 2)
                SendBytesReceived(r);
        }
        /*RTMP_Log(RTMP_LOGDEBUG, "%s: %d bytes\n", __FUNCTION__, nBytes); */
#ifdef _DEBUG
        fwrite(ptr, 1, nBytes, netstackdump_read);
#endif

        if (nBytes == 0) {
            RTMP_Log(RTMP_LOGDEBUG, "%s, RTMP socket closed by peer", __FUNCTION__);
            /*goto again; */
            RTMP_Close(r);
            break;
        }

        if (r->Link.protocol & RTMP_FEATURE_HTTP)
            r->m_resplen -= nBytes;

#ifdef CRYPTO
        if (r->Link.rc4keyIn)
    {
      RC4_encrypt(r->Link.rc4keyIn, nBytes, ptr);
    }
#endif

        n -= nBytes;
        ptr += nBytes;
    }

    return nOriginalSize - n;
}

static int
WriteN(RTMP *r, const char *buffer, int n) {
    const char *ptr = buffer;
#ifdef CRYPTO
    char *encrypted = 0;
  char buf[RTMP_BUFFER_CACHE_SIZE];

  if (r->Link.rc4keyOut)
    {
      if (n > sizeof(buf))
    encrypted = (char *)malloc(n);
      else
    encrypted = (char *)buf;
      ptr = encrypted;
      RC4_encrypt2(r->Link.rc4keyOut, n, buffer, ptr);
    }
#endif

    while (n > 0) {
        int nBytes;

        if (r->Link.protocol & RTMP_FEATURE_HTTP) {
            nBytes = HTTP_Post(r, RTMPT_SEND, ptr, n);
        } else {
            nBytes = RTMPSockBuf_Send(&r->m_sb, ptr, n);
        }
        /*RTMP_Log(RTMP_LOGDEBUG, "%s: %d\n", __FUNCTION__, nBytes); */

        if (nBytes < 0) {
            int sockerr = GetSockError();
            RTMP_Log(RTMP_LOGERROR, "%s, RTMP send error %d (%d bytes)", __FUNCTION__,
                     sockerr, n);

            if (sockerr == EINTR && !RTMP_ctrlC)
                continue;
            /**
             * TODO 意外断网 阻止递归调用
             */
            //RTMP_Close(r);
            n = 1;
            break;
        }

        if (nBytes == 0)
            break;

        n -= nBytes;
        ptr += nBytes;
    }

#ifdef CRYPTO
    if (encrypted && encrypted != buf)
    free(encrypted);
#endif

    return n == 0;
}

#ifndef CRYPTO

static int
HandShake(RTMP *r, int FP9HandShake) {
    int i;
    uint32_t uptime, suptime;
    int bMatch;
    char type;
    char clientbuf[RTMP_SIG_SIZE + 1], *clientsig = clientbuf + 1;
    char serversig[RTMP_SIG_SIZE];

    clientbuf[0] = 0x03;        /* not encrypted */

    uptime = htonl(RTMP_GetTime());
    memcpy(clientsig, &uptime, 4);

    memset(&clientsig[4], 0, 4);

#ifdef _DEBUG
    for (i = 8; i < RTMP_SIG_SIZE; i++)
    clientsig[i] = 0xff;
#else
    for (i = 8; i < RTMP_SIG_SIZE; i++)
        clientsig[i] = (char) (rand() % 256);
#endif

    if (!WriteN(r, clientbuf, RTMP_SIG_SIZE + 1))
        return FALSE;

    if (ReadN(r, &type, 1) != 1)    /* 0x03 or 0x06 */
        return FALSE;

    RTMP_Log(RTMP_LOGDEBUG, "%s: Type Answer   : %02X", __FUNCTION__, type);

    if (type != clientbuf[0])
        RTMP_Log(RTMP_LOGWARNING, "%s: Type mismatch: client sent %d, server answered %d",
                 __FUNCTION__, clientbuf[0], type);

    if (ReadN(r, serversig, RTMP_SIG_SIZE) != RTMP_SIG_SIZE)
        return FALSE;

    /* decode server response */

    memcpy(&suptime, serversig, 4);
    suptime = ntohl(suptime);

    RTMP_Log(RTMP_LOGDEBUG, "%s: Server Uptime : %d", __FUNCTION__, suptime);
    RTMP_Log(RTMP_LOGDEBUG, "%s: FMS Version   : %d.%d.%d.%d", __FUNCTION__,
             serversig[4], serversig[5], serversig[6], serversig[7]);

    /* 2nd part of handshake */
    if (!WriteN(r, serversig, RTMP_SIG_SIZE))
        return FALSE;

    if (ReadN(r, serversig, RTMP_SIG_SIZE) != RTMP_SIG_SIZE)
        return FALSE;

    bMatch = (memcmp(serversig, clientsig, RTMP_SIG_SIZE) == 0);
    if (!bMatch) {
        RTMP_Log(RTMP_LOGWARNING, "%s, client signature does not match!", __FUNCTION__);
    }
    return TRUE;
}

#endif

static int
SocksNegotiate(RTMP *r) {
    unsigned long addr;
    struct sockaddr_in service;
    memset(&service, 0, sizeof(struct sockaddr_in));

    add_addr_info(&service, &r->Link.hostname, r->Link.port);
    addr = htonl(service.sin_addr.s_addr);

    {
        char packet[] = {
                4, 1,            /* SOCKS 4, connect */
                (r->Link.port >> 8) & 0xFF,
                (r->Link.port) & 0xFF,
                (char) (addr >> 24) & 0xFF, (char) (addr >> 16) & 0xFF,
                (char) (addr >> 8) & 0xFF, (char) addr & 0xFF,
                0
        };                /* NULL terminate */

        WriteN(r, packet, sizeof packet);

        if (ReadN(r, packet, 8) != 8)
            return FALSE;

        if (packet[0] == 0 && packet[1] == 90) {
            return TRUE;
        } else {
            RTMP_Log(RTMP_LOGERROR, "%s, SOCKS returned error code %d", packet[1]);
            return FALSE;
        }
    }
}

int
RTMP_ClientPacket(RTMP *r, RTMPPacket *packet) {
    int bHasMediaPacket = 0;
    switch (packet->m_packetType) {
        case 0x01:
            /* chunk size */
            HandleChangeChunkSize(r, packet);
            break;

        case 0x03:
            /* bytes read report */
            RTMP_Log(RTMP_LOGDEBUG, "%s, received: bytes read report", __FUNCTION__);
            break;

        case 0x04:
            /* ctrl */
            HandleCtrl(r, packet);
            break;

        case 0x05:
            /* server bw */
            HandleServerBW(r, packet);
            break;

        case 0x06:
            /* client bw */
            HandleClientBW(r, packet);
            break;

        case 0x08:
            /* audio data */
            /*RTMP_Log(RTMP_LOGDEBUG, "%s, received: audio %lu bytes", __FUNCTION__, packet.m_nBodySize); */
            HandleAudio(r, packet);
            bHasMediaPacket = 1;
            if (!r->m_mediaChannel)
                r->m_mediaChannel = packet->m_nChannel;
            if (!r->m_pausing)
                r->m_mediaStamp = packet->m_nTimeStamp;
            break;

        case 0x09:
            /* video data */
            /*RTMP_Log(RTMP_LOGDEBUG, "%s, received: video %lu bytes", __FUNCTION__, packet.m_nBodySize); */
            HandleVideo(r, packet);
            bHasMediaPacket = 1;
            if (!r->m_mediaChannel)
                r->m_mediaChannel = packet->m_nChannel;
            if (!r->m_pausing)
                r->m_mediaStamp = packet->m_nTimeStamp;
            break;

        case 0x0F:            /* flex stream send */
            RTMP_Log(RTMP_LOGDEBUG,
                     "%s, flex stream send, size %lu bytes, not supported, ignoring",
                     __FUNCTION__, packet->m_nBodySize);
            break;

        case 0x10:            /* flex shared object */
            RTMP_Log(RTMP_LOGDEBUG,
                     "%s, flex shared object, size %lu bytes, not supported, ignoring",
                     __FUNCTION__, packet->m_nBodySize);
            break;

        case 0x11:            /* flex message */
        {
            RTMP_Log(RTMP_LOGDEBUG,
                     "%s, flex message, size %lu bytes, not fully supported",
                     __FUNCTION__, packet->m_nBodySize);
            /*RTMP_LogHex(packet.m_body, packet.m_nBodySize); */

            /* some DEBUG code */
#if 0
            RTMP_LIB_AMFObject obj;
       int nRes = obj.Decode(packet.m_body+1, packet.m_nBodySize-1);
       if(nRes < 0) {
       RTMP_Log(RTMP_LOGERROR, "%s, error decoding AMF3 packet", __FUNCTION__);
       /*return; */
       }

       obj.Dump();
#endif

            if (HandleInvoke(r, packet->m_body + 1, packet->m_nBodySize - 1) == 1)
                bHasMediaPacket = 2;
            break;
        }
        case 0x12:
            /* metadata (notify) */
            RTMP_Log(RTMP_LOGDEBUG, "%s, received: notify %lu bytes", __FUNCTION__,
                     packet->m_nBodySize);
            if (HandleMetadata(r, packet->m_body, packet->m_nBodySize))
                bHasMediaPacket = 1;
            break;

        case 0x13:
            RTMP_Log(RTMP_LOGDEBUG, "%s, shared object, not supported, ignoring",
                     __FUNCTION__);
            break;

        case 0x14:
            /* invoke */
            RTMP_Log(RTMP_LOGDEBUG, "%s, received: invoke %lu bytes", __FUNCTION__,
                     packet->m_nBodySize);
            /*RTMP_LogHex(packet.m_body, packet.m_nBodySize); */

            if (HandleInvoke(r, packet->m_body, packet->m_nBodySize) == 1)
                bHasMediaPacket = 2;
            break;

        case 0x16: {
            /* go through FLV packets and handle metadata packets */
            unsigned int pos = 0;
            uint32_t nTimeStamp = packet->m_nTimeStamp;

            while (pos + 11 < packet->m_nBodySize) {
                uint32_t dataSize = AMF_DecodeInt24(packet->m_body + pos + 1);    /* size without header (11) and prevTagSize (4) */

                if (pos + 11 + dataSize + 4 > packet->m_nBodySize) {
                    RTMP_Log(RTMP_LOGWARNING, "Stream corrupt?!");
                    break;
                }
                if (packet->m_body[pos] == 0x12) {
                    HandleMetadata(r, packet->m_body + pos + 11, dataSize);
                } else if (packet->m_body[pos] == 8 || packet->m_body[pos] == 9) {
                    nTimeStamp = AMF_DecodeInt24(packet->m_body + pos + 4);
                    nTimeStamp |= (packet->m_body[pos + 7] << 24);
                }
                pos += (11 + dataSize + 4);
            }
            if (!r->m_pausing)
                r->m_mediaStamp = nTimeStamp;

            /* FLV tag(s) */
            /*RTMP_Log(RTMP_LOGDEBUG, "%s, received: FLV tag(s) %lu bytes", __FUNCTION__, packet.m_nBodySize); */
            bHasMediaPacket = 1;
            break;
        }
        default:
            RTMP_Log(RTMP_LOGDEBUG, "%s, unknown packet type received: 0x%02x", __FUNCTION__,
                     packet->m_packetType);
#ifdef _DEBUG
            RTMP_LogHex(RTMP_LOGDEBUG, packet->m_body, packet->m_nBodySize);
#endif
    }

    return bHasMediaPacket;
}

int
RTMP_ReadPacket(RTMP *r, RTMPPacket *packet) {
    uint8_t hbuf[RTMP_MAX_HEADER_SIZE] = {0};
    char *header = (char *) hbuf;
    int nSize, hSize, nToRead, nChunk;
    int didAlloc = FALSE;

    RTMP_Log(RTMP_LOGDEBUG2, "%s: fd=%d", __FUNCTION__, r->m_sb.sb_socket);

    if (ReadN(r, (char *) hbuf, 1) == 0) {
        RTMP_Log(RTMP_LOGERROR, "%s, failed to read RTMP packet header", __FUNCTION__);
        return FALSE;
    }

    packet->m_headerType = (hbuf[0] & 0xc0) >> 6;
    packet->m_nChannel = (hbuf[0] & 0x3f);
    header++;
    if (packet->m_nChannel == 0) {
        if (ReadN(r, (char *) &hbuf[1], 1) != 1) {
            RTMP_Log(RTMP_LOGERROR, "%s, failed to read RTMP packet header 2nd byte",
                     __FUNCTION__);
            return FALSE;
        }
        packet->m_nChannel = hbuf[1];
        packet->m_nChannel += 64;
        header++;
    } else if (packet->m_nChannel == 1) {
        int tmp;
        if (ReadN(r, (char *) &hbuf[1], 2) != 2) {
            RTMP_Log(RTMP_LOGERROR, "%s, failed to read RTMP packet header 3nd byte",
                     __FUNCTION__);
            return FALSE;
        }
        tmp = (hbuf[2] << 8) + hbuf[1];
        packet->m_nChannel = tmp + 64;
        RTMP_Log(RTMP_LOGDEBUG, "%s, m_nChannel: %0x", __FUNCTION__, packet->m_nChannel);
        header += 2;
    }

    nSize = packetSize[packet->m_headerType];

    if (nSize == RTMP_LARGE_HEADER_SIZE)    /* if we get a full header the timestamp is absolute */
        packet->m_hasAbsTimestamp = TRUE;

    else if (nSize < RTMP_LARGE_HEADER_SIZE) {                /* using values from the last message of this channel */
        if (r->m_vecChannelsIn[packet->m_nChannel])
            memcpy(packet, r->m_vecChannelsIn[packet->m_nChannel],
                   sizeof(RTMPPacket));
    }

    nSize--;

    if (nSize > 0 && ReadN(r, header, nSize) != nSize) {
        RTMP_Log(RTMP_LOGERROR, "%s, failed to read RTMP packet header. type: %x",
                 __FUNCTION__, (unsigned int) hbuf[0]);
        return FALSE;
    }

    hSize = nSize + (header - (char *) hbuf);

    if (nSize >= 3) {
        packet->m_nTimeStamp = AMF_DecodeInt24(header);

        /*RTMP_Log(RTMP_LOGDEBUG, "%s, reading RTMP packet chunk on channel %x, headersz %i, timestamp %i, abs timestamp %i", __FUNCTION__, packet.m_nChannel, nSize, packet.m_nTimeStamp, packet.m_hasAbsTimestamp); */

        if (nSize >= 6) {
            packet->m_nBodySize = AMF_DecodeInt24(header + 3);
            packet->m_nBytesRead = 0;
            RTMPPacket_Free(packet);

            if (nSize > 6) {
                packet->m_packetType = header[6];

                if (nSize == 11)
                    packet->m_nInfoField2 = DecodeInt32LE(header + 7);
            }
        }
        if (packet->m_nTimeStamp == 0xffffff) {
            if (ReadN(r, header + nSize, 4) != 4) {
                RTMP_Log(RTMP_LOGERROR, "%s, failed to read extended timestamp",
                         __FUNCTION__);
                return FALSE;
            }
            packet->m_nTimeStamp = AMF_DecodeInt32(header + nSize);
            hSize += 4;
        }
    }

    RTMP_LogHexString(RTMP_LOGDEBUG2, (uint8_t *) hbuf, hSize);

    if (packet->m_nBodySize > 0 && packet->m_body == NULL) {
        if (!RTMPPacket_Alloc(packet, packet->m_nBodySize)) {
            RTMP_Log(RTMP_LOGDEBUG, "%s, failed to allocate packet", __FUNCTION__);
            return FALSE;
        }
        didAlloc = TRUE;
        packet->m_headerType = (hbuf[0] & 0xc0) >> 6;
    }

    nToRead = packet->m_nBodySize - packet->m_nBytesRead;
    nChunk = r->m_inChunkSize;
    if (nToRead < nChunk)
        nChunk = nToRead;

    /* Does the caller want the raw chunk? */
    if (packet->m_chunk) {
        packet->m_chunk->c_headerSize = hSize;
        memcpy(packet->m_chunk->c_header, hbuf, hSize);
        packet->m_chunk->c_chunk = packet->m_body + packet->m_nBytesRead;
        packet->m_chunk->c_chunkSize = nChunk;
    }

    if (ReadN(r, packet->m_body + packet->m_nBytesRead, nChunk) != nChunk) {
        RTMP_Log(RTMP_LOGERROR, "%s, failed to read RTMP packet body. len: %lu",
                 __FUNCTION__, packet->m_nBodySize);
        return FALSE;
    }

    RTMP_LogHexString(RTMP_LOGDEBUG2, (uint8_t *) packet->m_body + packet->m_nBytesRead, nChunk);

    packet->m_nBytesRead += nChunk;

    /* keep the packet as ref for other packets on this channel */
    if (!r->m_vecChannelsIn[packet->m_nChannel])
        r->m_vecChannelsIn[packet->m_nChannel] = malloc(sizeof(RTMPPacket));
    memcpy(r->m_vecChannelsIn[packet->m_nChannel], packet, sizeof(RTMPPacket));

    if (RTMPPacket_IsReady(packet)) {
        /* make packet's timestamp absolute */
        if (!packet->m_hasAbsTimestamp)
            packet->m_nTimeStamp += r->m_channelTimestamp[packet->m_nChannel];    /* timestamps seem to be always relative!! */

        r->m_channelTimestamp[packet->m_nChannel] = packet->m_nTimeStamp;

        /* reset the data from the stored packet. we keep the header since we may use it later if a new packet for this channel */
        /* arrives and requests to re-use some info (small packet header) */
        r->m_vecChannelsIn[packet->m_nChannel]->m_body = NULL;
        r->m_vecChannelsIn[packet->m_nChannel]->m_nBytesRead = 0;
        r->m_vecChannelsIn[packet->m_nChannel]->m_hasAbsTimestamp = FALSE;    /* can only be false if we reuse header */
    } else {
        packet->m_body = NULL;    /* so it won't be erased on free */
    }

    return TRUE;
}

int
RTMP_SendPacket(RTMP *r, RTMPPacket *packet, int queue) {
    const RTMPPacket *prevPacket = r->m_vecChannelsOut[packet->m_nChannel];
    uint32_t last = 0;
    int nSize;
    int hSize, cSize;
    char *header, *hptr, *hend, hbuf[RTMP_MAX_HEADER_SIZE], c;
    uint32_t t;
    char *buffer, *tbuf = NULL, *toff = NULL;
    int nChunkSize;
    int tlen;

    if (prevPacket && packet->m_headerType != RTMP_PACKET_SIZE_LARGE) {
        /* compress a bit by using the prev packet's attributes */
        if (prevPacket->m_nBodySize == packet->m_nBodySize
                && prevPacket->m_packetType == packet->m_packetType
                && packet->m_headerType == RTMP_PACKET_SIZE_MEDIUM)
            packet->m_headerType = RTMP_PACKET_SIZE_SMALL;

        if (prevPacket->m_nTimeStamp == packet->m_nTimeStamp
                && packet->m_headerType == RTMP_PACKET_SIZE_SMALL)
            packet->m_headerType = RTMP_PACKET_SIZE_MINIMUM;
        last = prevPacket->m_nTimeStamp;
    }

    if (packet->m_headerType > 3)    /* sanity */
    {
        RTMP_Log(RTMP_LOGERROR, "sanity failed!! trying to send header of type: 0x%02x.",
                 (unsigned char) packet->m_headerType);
        return FALSE;
    }

    nSize = packetSize[packet->m_headerType];
    hSize = nSize;
    cSize = 0;
    t = packet->m_nTimeStamp - last;

    if (packet->m_body) {
        header = packet->m_body - nSize;
        hend = packet->m_body;
    } else {
        header = hbuf + 6;
        hend = hbuf + sizeof(hbuf);
    }

    if (packet->m_nChannel > 319)
        cSize = 2;
    else if (packet->m_nChannel > 63)
        cSize = 1;
    if (cSize) {
        header -= cSize;
        hSize += cSize;
    }

    if (nSize > 1 && t >= 0xffffff) {
        header -= 4;
        hSize += 4;
    }

    hptr = header;
    c = packet->m_headerType << 6;
    switch (cSize) {
        case 0:c |= packet->m_nChannel;
            break;
        case 1:break;
        case 2:c |= 1;
            break;
    }
    *hptr++ = c;
    if (cSize) {
        int tmp = packet->m_nChannel - 64;
        *hptr++ = tmp & 0xff;
        if (cSize == 2)
            *hptr++ = tmp >> 8;
    }

    if (nSize > 1) {
        hptr = AMF_EncodeInt24(hptr, hend, t > 0xffffff ? 0xffffff : t);
    }

    if (nSize > 4) {
        hptr = AMF_EncodeInt24(hptr, hend, packet->m_nBodySize);
        *hptr++ = packet->m_packetType;
    }

    if (nSize > 8)
        hptr += EncodeInt32LE(hptr, packet->m_nInfoField2);

    if (nSize > 1 && t >= 0xffffff)
        hptr = AMF_EncodeInt32(hptr, hend, t);

    nSize = packet->m_nBodySize;
    buffer = packet->m_body;
    nChunkSize = r->m_outChunkSize;

    RTMP_Log(RTMP_LOGDEBUG2, "%s: fd=%d, size=%d", __FUNCTION__, r->m_sb.sb_socket,
             nSize);
    /* send all chunks in one HTTP request */
    if (r->Link.protocol & RTMP_FEATURE_HTTP) {
        int chunks = (nSize + nChunkSize - 1) / nChunkSize;
        if (chunks > 1) {
            tlen = chunks * (cSize + 1) + nSize + hSize;
            tbuf = malloc(tlen);
            if (!tbuf)
                return FALSE;
            toff = tbuf;
        }
    }
    while (nSize + hSize) {
        int wrote;

        if (nSize < nChunkSize)
            nChunkSize = nSize;

        RTMP_LogHexString(RTMP_LOGDEBUG2, (uint8_t *) header, hSize);
        RTMP_LogHexString(RTMP_LOGDEBUG2, (uint8_t *) buffer, nChunkSize);
        if (tbuf) {
            memcpy(toff, header, nChunkSize + hSize);
            toff += nChunkSize + hSize;
        } else {
            wrote = WriteN(r, header, nChunkSize + hSize);
            if (!wrote)
                return FALSE;

        }
        nSize -= nChunkSize;
        buffer += nChunkSize;
        hSize = 0;

        if (nSize > 0) {
            header = buffer - 1;
            hSize = 1;
            if (cSize) {
                header -= cSize;
                hSize += cSize;
            }
            *header = (0xc0 | c);
            if (cSize) {
                int tmp = packet->m_nChannel - 64;
                header[1] = tmp & 0xff;
                if (cSize == 2)
                    header[2] = tmp >> 8;
            }
        }
    }
    if (tbuf) {
        int wrote = WriteN(r, tbuf, toff - tbuf);
        free(tbuf);
        tbuf = NULL;
        if (!wrote)
            return FALSE;
    }

    /* we invoked a remote method */
    if (packet->m_packetType == 0x14) {
        AVal method;
        char *ptr;
        ptr = packet->m_body + 1;
        AMF_DecodeString(ptr, &method);
        RTMP_Log(RTMP_LOGDEBUG, "Invoking %s", method.av_val);
        /* keep it in call queue till result arrives */
        if (queue) {
            int txn;
            ptr += 3 + method.av_len;
            txn = (int) AMF_DecodeNumber(ptr);
            AV_queue(&r->m_methodCalls, &r->m_numCalls, &method, txn);
        }
    }

    if (!r->m_vecChannelsOut[packet->m_nChannel])
        r->m_vecChannelsOut[packet->m_nChannel] = malloc(sizeof(RTMPPacket));
    memcpy(r->m_vecChannelsOut[packet->m_nChannel], packet, sizeof(RTMPPacket));
    return TRUE;
}

int RTMP_SetOpt(RTMP *r, const AVal *opt, AVal *arg) {
    int i;
    void *v;

    for (i = 0; options[i].name.av_len; i++) {
        if (opt->av_len != options[i].name.av_len) continue;
        if (strcasecmp(opt->av_val, options[i].name.av_val)) continue;
        v = (char *) r + options[i].off;
        switch (options[i].otype) {
            case OPT_STR: {
                AVal *aptr = v;
                *aptr = *arg;
            }
                break;
            case OPT_INT: {
                long l = strtol(arg->av_val, NULL, 0);
                *(int *) v = l;
            }
                break;
            case OPT_BOOL: {
                int j, fl;
                fl = *(int *) v;
                for (j = 0; truth[j].av_len; j++) {
                    if (arg->av_len != truth[j].av_len) continue;
                    if (strcasecmp(arg->av_val, truth[j].av_val)) continue;
                    fl |= options[i].omisc;
                    break;
                }
                *(int *) v = fl;
            }
                break;
            case OPT_CONN:
                if (parseAMF(&r->Link.extras, arg, &r->Link.edepth))
                    return FALSE;
                break;
        }
        break;
    }
    if (!options[i].name.av_len) {
        RTMP_Log(RTMP_LOGERROR, "Unknown option %s", opt->av_val);
        RTMP_OptUsage();
        return FALSE;
    }

    return TRUE;
}

int RTMP_SetupURL(RTMP *r, char *url) {
    AVal opt, arg;
    char *p1, *p2, *ptr = strchr(url, ' ');
    int ret, len;
    unsigned int port = 0;

    if (ptr)
        *ptr = '\0';

    len = strlen(url);
    ret = RTMP_ParseURL(url, &r->Link.protocol, &r->Link.hostname,
                        &port, &r->Link.playpath0, &r->Link.app);
    if (!ret)
        return ret;
    r->Link.port = port;
    r->Link.playpath = r->Link.playpath0;

    while (ptr) {
        *ptr++ = '\0';
        p1 = ptr;
        p2 = strchr(p1, '=');
        if (!p2)
            break;
        opt.av_val = p1;
        opt.av_len = p2 - p1;
        *p2++ = '\0';
        arg.av_val = p2;
        ptr = strchr(p2, ' ');
        if (ptr) {
            *ptr = '\0';
            arg.av_len = ptr - p2;
            /* skip repeated spaces */
            while (ptr[1] == ' ')
                *ptr++ = '\0';
        } else {
            arg.av_len = strlen(p2);
        }

        /* unescape */
        port = arg.av_len;
        for (p1 = p2; port > 0;) {
            if (*p1 == '\\') {
                unsigned int c;
                if (port < 3)
                    return FALSE;
                sscanf(p1 + 1, "%02x", &c);
                *p2++ = c;
                port -= 3;
                p1 += 3;
            } else {
                *p2++ = *p1++;
                port--;
            }
        }
        arg.av_len = p2 - arg.av_val;

        ret = RTMP_SetOpt(r, &opt, &arg);
        if (!ret)
            return ret;
    }

    if (!r->Link.tcUrl.av_len) {
        r->Link.tcUrl.av_val = url;
        if (r->Link.app.av_len) {
            if (r->Link.app.av_val < url + len) {
                /* if app is part of original url, just use it */
                r->Link.tcUrl.av_len = r->Link.app.av_len + (r->Link.app.av_val - url);
            } else {
                len = r->Link.hostname.av_len + r->Link.app.av_len +
                        sizeof("rtmpte://:65535/");
                r->Link.tcUrl.av_val = malloc(len);
                r->Link.tcUrl.av_len = snprintf(r->Link.tcUrl.av_val, len,
                                                "%s://%.*s:%d/%.*s",
                                                RTMPProtocolStringsLower[r->Link.protocol],
                                                r->Link.hostname.av_len, r->Link.hostname.av_val,
                                                r->Link.port,
                                                r->Link.app.av_len, r->Link.app.av_val);
                r->Link.lFlags |= RTMP_LF_FTCU;
            }
        } else {
            r->Link.tcUrl.av_len = strlen(url);
        }
    }

#ifdef CRYPTO
    if ((r->Link.lFlags & RTMP_LF_SWFV) && r->Link.swfUrl.av_len)
    RTMP_HashSWF(r->Link.swfUrl.av_val, &r->Link.SWFSize,
      (unsigned char *)r->Link.SWFHash, r->Link.swfAge);
#endif

    if (r->Link.port == 0) {
        if (r->Link.protocol & RTMP_FEATURE_SSL)
            r->Link.port = 443;
        else if (r->Link.protocol & RTMP_FEATURE_HTTP)
            r->Link.port = 80;
        else
            r->Link.port = 1935;
    }
    return TRUE;
}

void
RTMP_EnableWrite(RTMP *r) {
    r->Link.protocol |= RTMP_FEATURE_WRITE;
}

int
RTMP_SendServerBW(RTMP *r) {
    RTMPPacket packet;
    char pbuf[256], *pend = pbuf + sizeof(pbuf);

    packet.m_nChannel = 0x02;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet.m_packetType = 0x05;    /* Server BW */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    packet.m_nBodySize = 4;

    AMF_EncodeInt32(packet.m_body, pend, r->m_nServerBW);
    return RTMP_SendPacket(r, &packet, FALSE);
}

int
RTMP_SendCtrl(RTMP *r, short nType, unsigned int nObject, unsigned int nTime) {
    RTMPPacket packet;
    char pbuf[256], *pend = pbuf + sizeof(pbuf);
    int nSize;
    char *buf;

    RTMP_Log(RTMP_LOGDEBUG, "sending ctrl. type: 0x%04x", (unsigned short) nType);

    packet.m_nChannel = 0x02;    /* control channel (ping) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x04;    /* ctrl */
    packet.m_nTimeStamp = 0;    /* RTMP_GetTime(); */
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    switch (nType) {
        case 0x03: nSize = 10;
            break;    /* buffer time */
        case 0x1A: nSize = 3;
            break;    /* SWF verify request */
        case 0x1B: nSize = 44;
            break;    /* SWF verify response */
        default: nSize = 6;
            break;
    }

    packet.m_nBodySize = nSize;

    buf = packet.m_body;
    buf = AMF_EncodeInt16(buf, pend, nType);

    if (nType == 0x1B) {
#ifdef CRYPTO
        memcpy(buf, r->Link.SWFVerificationResponse, 42);
      RTMP_Log(RTMP_LOGDEBUG, "Sending SWFVerification response: ");
      RTMP_LogHex(RTMP_LOGDEBUG, (uint8_t *)packet.m_body, packet.m_nBodySize);
#endif
    } else if (nType == 0x1A) {
        *buf = nObject & 0xff;
    } else {
        if (nSize > 2)
            buf = AMF_EncodeInt32(buf, pend, nObject);

        if (nSize > 6)
            buf = AMF_EncodeInt32(buf, pend, nTime);
    }

    return RTMP_SendPacket(r, &packet, FALSE);
}

int
RTMP_SendCreateStream(RTMP *r) {
    RTMPPacket packet;
    char pbuf[256], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_createStream);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;        /* NULL */

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, TRUE);
}


static int
SendPlay(RTMP *r) {
    RTMPPacket packet;
    char pbuf[1024], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x08;    /* we make 8 our stream channel */
    packet.m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = r->m_stream_id;    /*0x01000000; */
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_play);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;

    RTMP_Log(RTMP_LOGDEBUG, "%s, seekTime=%d, stopTime=%d, sending play: %s",
             __FUNCTION__, r->Link.seekTime, r->Link.stopTime,
             r->Link.playpath.av_val);
    enc = AMF_EncodeString(enc, pend, &r->Link.playpath);
    if (!enc)
        return FALSE;

    /* Optional parameters start and len.
     *
     * start: -2, -1, 0, positive number
     *  -2: looks for a live stream, then a recorded stream,
     *      if not found any open a live stream
     *  -1: plays a live stream
     * >=0: plays a recorded streams from 'start' milliseconds
     */
    if (r->Link.lFlags & RTMP_LF_LIVE)
        enc = AMF_EncodeNumber(enc, pend, -1000.0);
    else {
        if (r->Link.seekTime > 0.0)
            enc = AMF_EncodeNumber(enc, pend, r->Link.seekTime);    /* resume from here */
        else
            enc = AMF_EncodeNumber(enc,
                                   pend,
                                   0.0);    /*-2000.0);*/ /* recorded as default, -2000.0 is not reliable since that freezes the player if the stream is not found */
    }
    if (!enc)
        return FALSE;

    /* len: -1, 0, positive number
     *  -1: plays live or recorded stream to the end (default)
     *   0: plays a frame 'start' ms away from the beginning
     *  >0: plays a live or recoded stream for 'len' milliseconds
     */
    /*enc += EncodeNumber(enc, -1.0); */ /* len */
    if (r->Link.stopTime) {
        enc = AMF_EncodeNumber(enc, pend, r->Link.stopTime - r->Link.seekTime);
        if (!enc)
            return FALSE;
    }

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, TRUE);
}

static int
SendCheckBWResult(RTMP *r, double txn) {
    RTMPPacket packet;
    char pbuf[256], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x03;    /* control channel (invoke) */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;    /* INVOKE */
    packet.m_nTimeStamp = 0x16 * r->m_nBWCheckCounter;    /* temp inc value. till we figure it out. */
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av__result);
    enc = AMF_EncodeNumber(enc, pend, txn);
    *enc++ = AMF_NULL;
    enc = AMF_EncodeNumber(enc, pend, (double) r->m_nBWCheckCounter++);

    packet.m_nBodySize = enc - packet.m_body;

    return RTMP_SendPacket(r, &packet, FALSE);
}

SAVC(pause);

int
RTMP_SendPause(RTMP *r, int DoPause, int iTime) {
    RTMPPacket packet;
    char pbuf[256], *pend = pbuf + sizeof(pbuf);
    char *enc;

    packet.m_nChannel = 0x08;    /* video channel */
    packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet.m_packetType = 0x14;    /* invoke */
    packet.m_nTimeStamp = 0;
    packet.m_nInfoField2 = 0;
    packet.m_hasAbsTimestamp = 0;
    packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

    enc = packet.m_body;
    enc = AMF_EncodeString(enc, pend, &av_pause);
    enc = AMF_EncodeNumber(enc, pend, ++r->m_numInvokes);
    *enc++ = AMF_NULL;
    enc = AMF_EncodeBoolean(enc, pend, DoPause);
    enc = AMF_EncodeNumber(enc, pend, (double) iTime);

    packet.m_nBodySize = enc - packet.m_body;

    RTMP_Log(RTMP_LOGDEBUG, "%s, %d, pauseTime=%d", __FUNCTION__, DoPause, iTime);
    return RTMP_SendPacket(r, &packet, TRUE);
}

int
RTMP_ConnectStream(RTMP *r, int seekTime) {
    RTMPPacket packet = {0};

    /* seekTime was already set by SetupStream / SetupURL.
     * This is only needed by ReconnectStream.
     */
    if (seekTime > 0)
        r->Link.seekTime = seekTime;

    r->m_mediaChannel = 0;

    while (!r->m_bPlaying && RTMP_IsConnected(r) && RTMP_ReadPacket(r, &packet)) {
        if (RTMPPacket_IsReady(&packet)) {
            if (!packet.m_nBodySize)
                continue;
            if ((packet.m_packetType == RTMP_PACKET_TYPE_AUDIO) ||
                    (packet.m_packetType == RTMP_PACKET_TYPE_VIDEO) ||
                    (packet.m_packetType == RTMP_PACKET_TYPE_INFO)) {
                RTMP_Log(RTMP_LOGWARNING, "Received FLV packet before play()! Ignoring.");
                RTMPPacket_Free(&packet);
                continue;
            }

            RTMP_ClientPacket(r, &packet);
            RTMPPacket_Free(&packet);
        }
    }

    return r->m_bPlaying;
}

int
RTMP_Connect0(RTMP *r, struct sockaddr *service) {
    int on = 1;
    r->m_sb.sb_timedout = FALSE;
    r->m_pausing = 0;
    r->m_fDuration = 0.0;

    r->m_sb.sb_socket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (r->m_sb.sb_socket != -1) {
        SET_RCVTIMEO(tv, r->Link.timeout);
        if (setsockopt
                (r->m_sb.sb_socket, SOL_SOCKET, SO_SNDTIMEO, (char *) &tv, sizeof(tv))) {
            RTMP_Log(RTMP_LOGERROR, "%s, Setting socket timeout to %ds failed!",
                     __FUNCTION__, r->Link.timeout);
        }

        if (connect(r->m_sb.sb_socket, service, sizeof(struct sockaddr)) < 0) {
            int err = GetSockError();
            RTMP_Log(RTMP_LOGERROR, "%s, failed to connect socket. %d (%s)",
                     __FUNCTION__, err, strerror(err));
            RTMP_Close(r);
            return FALSE;
        }

        if (r->Link.socksport) {
            RTMP_Log(RTMP_LOGDEBUG, "%s ... SOCKS negotiation", __FUNCTION__);
            if (!SocksNegotiate(r)) {
                RTMP_Log(RTMP_LOGERROR, "%s, SOCKS negotiation failed.", __FUNCTION__);
                RTMP_Close(r);
                return FALSE;
            }
        }
    } else {
        RTMP_Log(RTMP_LOGERROR, "%s, failed to create socket. Error: %d", __FUNCTION__,
                 GetSockError());
        return FALSE;
    }

    /* set timeout */
    {
        SET_RCVTIMEO(tv, r->Link.timeout);
        if (setsockopt
                (r->m_sb.sb_socket, SOL_SOCKET, SO_RCVTIMEO, (char *) &tv, sizeof(tv))) {
            RTMP_Log(RTMP_LOGERROR, "%s, Setting socket timeout to %ds failed!",
                     __FUNCTION__, r->Link.timeout);
        }
    }

    setsockopt(r->m_sb.sb_socket, IPPROTO_TCP, TCP_NODELAY, (char *) &on, sizeof(on));

    return TRUE;
}

int
RTMP_Connect1(RTMP *r, RTMPPacket *cp) {
    if (r->Link.protocol & RTMP_FEATURE_SSL) {
#if defined(CRYPTO) && !defined(NO_SSL)
        TLS_client(RTMP_TLS_ctx, r->m_sb.sb_ssl);
      TLS_setfd(r->m_sb.sb_ssl, r->m_sb.sb_socket);
      if (TLS_connect(r->m_sb.sb_ssl) < 0)
    {
      RTMP_Log(RTMP_LOGERROR, "%s, TLS_Connect failed", __FUNCTION__);
      RTMP_Close(r);
      return FALSE;
    }
#else
        RTMP_Log(RTMP_LOGERROR, "%s, no SSL/TLS support", __FUNCTION__);
        RTMP_Close(r);
        return FALSE;

#endif
    }
    if (r->Link.protocol & RTMP_FEATURE_HTTP) {
        r->m_msgCounter = 1;
        r->m_clientID.av_val = NULL;
        r->m_clientID.av_len = 0;
        HTTP_Post(r, RTMPT_OPEN, "", 1);
        HTTP_read(r, 1);
        r->m_msgCounter = 0;
    }
    RTMP_Log(RTMP_LOGDEBUG, "%s, ... connected, handshaking", __FUNCTION__);
    if (!HandShake(r, TRUE)) {
        RTMP_Log(RTMP_LOGERROR, "%s, handshake failed.", __FUNCTION__);
        RTMP_Close(r);
        return FALSE;
    }
    RTMP_Log(RTMP_LOGDEBUG, "%s, handshaked", __FUNCTION__);

    if (!SendConnectPacket(r, cp)) {
        RTMP_Log(RTMP_LOGERROR, "%s, RTMP connect failed.", __FUNCTION__);
        RTMP_Close(r);
        return FALSE;
    }
    return TRUE;
}

int
RTMP_Connect(RTMP *r, RTMPPacket *cp) {
    struct sockaddr_in service;
    if (!r->Link.hostname.av_len)
        return FALSE;


    memset(&service, 0, sizeof(struct sockaddr_in));
    service.sin_family = AF_INET;

    if (r->Link.socksport) {
        /* Connect via SOCKS */
        if (!add_addr_info(&service, &r->Link.sockshost, r->Link.socksport))
            return FALSE;

    } else {
        /* Connect directly */
        if (!add_addr_info(&service, &r->Link.hostname, r->Link.port))
            return FALSE;

    }

    if (!RTMP_Connect0(r, (struct sockaddr *) &service))
        return FALSE;


    r->m_bSendCounter = TRUE;

    return RTMP_Connect1(r, cp);
}

int
RTMP_IsConnected(RTMP *r) {
    return r->m_sb.sb_socket != -1;
}

uint32_t
RTMP_GetTime() {
#ifdef _DEBUG
    return 0;
#elif defined(_WIN32)
    return timeGetTime();
#else
    struct tms t;
    if (!clk_tck) clk_tck = sysconf(_SC_CLK_TCK);
    return times(&t) * 1000 / clk_tck;
#endif
}

int
RTMPPacket_Alloc(RTMPPacket *p, int nSize) {
    char *ptr = calloc(1, nSize + RTMP_MAX_HEADER_SIZE);
    if (!ptr)
        return FALSE;
    p->m_body = ptr + RTMP_MAX_HEADER_SIZE;
    p->m_nBytesRead = 0;
    return TRUE;
}

void
RTMPPacket_Free(RTMPPacket *p) {
    if (p->m_body) {
        free(p->m_body - RTMP_MAX_HEADER_SIZE);
        p->m_body = NULL;
    }
}


RTMP *
RTMP_Alloc() {
    return calloc(1, sizeof(RTMP));
}

void
RTMP_Free(RTMP *r) {
    free(r);
}

void
RTMP_Init(RTMP *r) {
#ifdef CRYPTO
    if (!RTMP_TLS_ctx)
    RTMP_TLS_Init();
#endif

    memset(r, 0, sizeof(RTMP));
    r->m_sb.sb_socket = -1;
    r->m_inChunkSize = RTMP_DEFAULT_CHUNKSIZE;
    r->m_outChunkSize = RTMP_DEFAULT_CHUNKSIZE;
    r->m_nBufferMS = 30000;
    r->m_nClientBW = 2500000;
    r->m_nClientBW2 = 2;
    r->m_nServerBW = 2500000;
    r->m_fAudioCodecs = 3191.0;
    r->m_fVideoCodecs = 252.0;
    r->Link.timeout = 30;
    r->Link.swfAge = 30;
}

void
RTMP_Close(RTMP *r) {
    int i;

    if (RTMP_IsConnected(r)) {
        if (r->m_stream_id > 0) {
            if ((r->Link.protocol & RTMP_FEATURE_WRITE))
                SendFCUnpublish(r);
            i = r->m_stream_id;
            r->m_stream_id = 0;
            SendDeleteStream(r, i);
        }
        if (r->m_clientID.av_val) {
            HTTP_Post(r, RTMPT_CLOSE, "", 1);
            free(r->m_clientID.av_val);
            r->m_clientID.av_val = NULL;
            r->m_clientID.av_len = 0;
        }
        RTMPSockBuf_Close(&r->m_sb);
    }

    r->m_stream_id = -1;
    r->m_sb.sb_socket = -1;
    r->m_nBWCheckCounter = 0;
    r->m_nBytesIn = 0;
    r->m_nBytesInSent = 0;

    if (r->m_read.flags & RTMP_READ_HEADER) {
        free(r->m_read.buf);
        r->m_read.buf = NULL;
    }
    r->m_read.dataType = 0;
    r->m_read.flags = 0;
    r->m_read.status = 0;
    r->m_read.nResumeTS = 0;
    r->m_read.nIgnoredFrameCounter = 0;
    r->m_read.nIgnoredFlvFrameCounter = 0;

    r->m_write.m_nBytesRead = 0;
    RTMPPacket_Free(&r->m_write);

    for (i = 0; i < RTMP_CHANNELS; i++) {
        if (r->m_vecChannelsIn[i]) {
            RTMPPacket_Free(r->m_vecChannelsIn[i]);
            free(r->m_vecChannelsIn[i]);
            r->m_vecChannelsIn[i] = NULL;
        }
        if (r->m_vecChannelsOut[i]) {
            free(r->m_vecChannelsOut[i]);
            r->m_vecChannelsOut[i] = NULL;
        }
    }
    AV_clear(r->m_methodCalls, r->m_numCalls);
    r->m_methodCalls = NULL;
    r->m_numCalls = 0;
    r->m_numInvokes = 0;

    r->m_bPlaying = FALSE;
    r->m_sb.sb_size = 0;

    r->m_msgCounter = 0;
    r->m_resplen = 0;
    r->m_unackd = 0;

    free(r->Link.playpath0.av_val);
    r->Link.playpath0.av_val = NULL;

    if (r->Link.lFlags & RTMP_LF_FTCU) {
        free(r->Link.tcUrl.av_val);
        r->Link.tcUrl.av_val = NULL;
        r->Link.lFlags ^= RTMP_LF_FTCU;
    }

#ifdef CRYPTO
    if (r->Link.dh)
    {
      MDH_free(r->Link.dh);
      r->Link.dh = NULL;
    }
  if (r->Link.rc4keyIn)
    {
      RC4_free(r->Link.rc4keyIn);
      r->Link.rc4keyIn = NULL;
    }
  if (r->Link.rc4keyOut)
    {
      RC4_free(r->Link.rc4keyOut);
      r->Link.rc4keyOut = NULL;
    }
#endif
}

int
RTMP_FindFirstMatchingProperty(AMFObject *obj, const AVal *name,
                               AMFObjectProperty *p) {
    int n;
    /* this is a small object search to locate the "duration" property */
    for (n = 0; n < obj->o_num; n++) {
        AMFObjectProperty *prop = AMF_GetProp(obj, NULL, n);

        if (AVMATCH(&prop->p_name, name)) {
            *p = *prop;
            return TRUE;
        }

        if (prop->p_type == AMF_OBJECT) {
            if (RTMP_FindFirstMatchingProperty(&prop->p_vu.p_object, name, p))
                return TRUE;
        }
    }
    return FALSE;
}

static int
DumpMetaData(AMFObject *obj) {
    AMFObjectProperty *prop;
    int n;
    for (n = 0; n < obj->o_num; n++) {
        prop = AMF_GetProp(obj, NULL, n);
        if (prop->p_type != AMF_OBJECT) {
            char str[256] = "";
            switch (prop->p_type) {
                case AMF_NUMBER:snprintf(str, 255, "%.2f", prop->p_vu.p_number);
                    break;
                case AMF_BOOLEAN:
                    snprintf(str, 255, "%s",
                             prop->p_vu.p_number != 0. ? "TRUE" : "FALSE");
                    break;
                case AMF_STRING:
                    snprintf(str, 255, "%.*s", prop->p_vu.p_aval.av_len,
                             prop->p_vu.p_aval.av_val);
                    break;
                case AMF_DATE:snprintf(str, 255, "timestamp:%.2f", prop->p_vu.p_number);
                    break;
                default:
                    snprintf(str, 255, "INVALID TYPE 0x%02x",
                             (unsigned char) prop->p_type);
            }
            if (prop->p_name.av_len) {
                /* chomp */
                if (strlen(str) >= 1 && str[strlen(str) - 1] == '\n')
                    str[strlen(str) - 1] = '\0';
                RTMP_Log(RTMP_LOGINFO, "  %-22.*s%s", prop->p_name.av_len,
                         prop->p_name.av_val, str);
            }
        } else {
            if (prop->p_name.av_len)
                RTMP_Log(RTMP_LOGINFO, "%.*s:", prop->p_name.av_len, prop->p_name.av_val);
            DumpMetaData(&prop->p_vu.p_object);
        }
    }
    return FALSE;
}

#define HEX2BIN(a)    (((a)&0x40)?((a)&0xf)+9:((a)&0xf))

static void
DecodeTEA(AVal *key, AVal *text) {
    uint32_t *v, k[4] = {0}, u;
    uint32_t z, y, sum = 0, e, DELTA = 0x9e3779b9;
    int32_t p, q;
    int i, n;
    unsigned char *ptr, *out;

    /* prep key: pack 1st 16 chars into 4 LittleEndian ints */
    ptr = (unsigned char *) key->av_val;
    u = 0;
    n = 0;
    v = k;
    p = key->av_len > 16 ? 16 : key->av_len;
    for (i = 0; i < p; i++) {
        u |= ptr[i] << (n * 8);
        if (n == 3) {
            *v++ = u;
            u = 0;
            n = 0;
        } else {
            n++;
        }
    }
    /* any trailing chars */
    if (u)
        *v = u;

    /* prep text: hex2bin, multiples of 4 */
    n = (text->av_len + 7) / 8;
    out = malloc(n * 8);
    ptr = (unsigned char *) text->av_val;
    v = (uint32_t *) out;
    for (i = 0; i < n; i++) {
        u = (HEX2BIN(ptr[0]) << 4) + HEX2BIN(ptr[1]);
        u |= ((HEX2BIN(ptr[2]) << 4) + HEX2BIN(ptr[3])) << 8;
        u |= ((HEX2BIN(ptr[4]) << 4) + HEX2BIN(ptr[5])) << 16;
        u |= ((HEX2BIN(ptr[6]) << 4) + HEX2BIN(ptr[7])) << 24;
        *v++ = u;
        ptr += 8;
    }
    v = (uint32_t *) out;

    /* http://www.movable-type.co.uk/scripts/tea-block.html */
#define MX (((z>>5)^(y<<2)) + ((y>>3)^(z<<4))) ^ ((sum^y) + (k[(p&3)^e]^z));
    z = v[n - 1];
    y = v[0];
    q = 6 + 52 / n;
    sum = q * DELTA;
    while (sum != 0) {
        e = sum >> 2 & 3;
        for (p = n - 1; p > 0; p--)
            z = v[p - 1], y = v[p] -= MX;
        z = v[n - 1];
        y = v[0] -= MX;
        sum -= DELTA;
    }

    text->av_len /= 2;
    memcpy(text->av_val, out, text->av_len);
    free(out);
}

static int
HTTP_Post(RTMP *r, RTMPTCmd cmd, const char *buf, int len) {
    char hbuf[512];
    int hlen = snprintf(hbuf, sizeof(hbuf), "POST /%s%s/%d HTTP/1.1\r\n"
                                            "Host: %.*s:%d\r\n"
                                            "Accept: */*\r\n"
                                            "User-Agent: Shockwave Flash\n"
                                            "Connection: Keep-Alive\n"
                                            "Cache-Control: no-cache\r\n"
                                            "Content-type: application/x-fcs\r\n"
                                            "Content-length: %d\r\n\r\n", RTMPT_cmds[cmd],
                        r->m_clientID.av_val ? r->m_clientID.av_val : "",
                        r->m_msgCounter, r->Link.hostname.av_len, r->Link.hostname.av_val,
                        r->Link.port, len);
    RTMPSockBuf_Send(&r->m_sb, hbuf, hlen);
    hlen = RTMPSockBuf_Send(&r->m_sb, buf, len);
    r->m_msgCounter++;
    r->m_unackd++;
    return hlen;
}


int RTMP_ParseURL(const char *url, int *protocol, AVal *host, unsigned int *port,
                  AVal *playpath, AVal *app) {
    char *p, *end, *col, *ques, *slash;

    RTMP_Log(RTMP_LOGDEBUG, "Parsing...");

    *protocol = RTMP_PROTOCOL_RTMP;
    *port = 0;
    playpath->av_len = 0;
    playpath->av_val = NULL;
    app->av_len = 0;
    app->av_val = NULL;

    /* Old School Parsing */

    /* look for usual :// pattern */
    p = strstr(url, "://");
    if (!p) {
        RTMP_Log(RTMP_LOGERROR, "RTMP URL: No :// in url!");
        return FALSE;
    }
    {
        int len = (int) (p - url);

        if (len == 4 && strncasecmp(url, "rtmp", 4) == 0)
            *protocol = RTMP_PROTOCOL_RTMP;
        else if (len == 5 && strncasecmp(url, "rtmpt", 5) == 0)
            *protocol = RTMP_PROTOCOL_RTMPT;
        else if (len == 5 && strncasecmp(url, "rtmps", 5) == 0)
            *protocol = RTMP_PROTOCOL_RTMPS;
        else if (len == 5 && strncasecmp(url, "rtmpe", 5) == 0)
            *protocol = RTMP_PROTOCOL_RTMPE;
        else if (len == 5 && strncasecmp(url, "rtmfp", 5) == 0)
            *protocol = RTMP_PROTOCOL_RTMFP;
        else if (len == 6 && strncasecmp(url, "rtmpte", 6) == 0)
            *protocol = RTMP_PROTOCOL_RTMPTE;
        else if (len == 6 && strncasecmp(url, "rtmpts", 6) == 0)
            *protocol = RTMP_PROTOCOL_RTMPTS;
        else {
            RTMP_Log(RTMP_LOGWARNING, "Unknown protocol!\n");
            goto parsehost;
        }
    }

    RTMP_Log(RTMP_LOGDEBUG, "Parsed protocol: %d", *protocol);

    parsehost:
    /* let's get the hostname */
    p += 3;

    /* check for sudden death */
    if (*p == 0) {
        RTMP_Log(RTMP_LOGWARNING, "No hostname in URL!");
        return FALSE;
    }

    end = p + strlen(p);
    col = strchr(p, ':');
    ques = strchr(p, '?');
    slash = strchr(p, '/');

    {
        int hostlen;
        if (slash)
            hostlen = slash - p;
        else
            hostlen = end - p;
        if (col && col - p < hostlen)
            hostlen = col - p;

        if (hostlen < 256) {
            host->av_val = p;
            host->av_len = hostlen;
            RTMP_Log(RTMP_LOGDEBUG, "Parsed host    : %.*s", hostlen, host->av_val);
        } else {
            RTMP_Log(RTMP_LOGWARNING, "Hostname exceeds 255 characters!");
        }

        p += hostlen;
    }

    /* get the port number if available */
    if (*p == ':') {
        unsigned int p2;
        p++;
        p2 = atoi(p);
        if (p2 > 65535) {
            RTMP_Log(RTMP_LOGWARNING, "Invalid port number!");
        } else {
            *port = p2;
        }
    }

    if (!slash) {
        RTMP_Log(RTMP_LOGWARNING, "No application or playpath in URL!");
        return TRUE;
    }
    p = slash + 1;

    {
        /* parse application
         *
         * rtmp://host[:port]/app[/appinstance][/...]
         * application = app[/appinstance]
         */

        char *slash2, *slash3 = NULL;
        int applen, appnamelen;

        slash2 = strchr(p, '/');
        if (slash2)
            slash3 = strchr(slash2 + 1, '/');

        applen = end - p; /* ondemand, pass all parameters as app */
        appnamelen = applen; /* ondemand length */

        if (ques && strstr(p,
                           "slist=")) { /* whatever it is, the '?' and slist= means we need to use everything as app and parse plapath from slist= */
            appnamelen = ques - p;
        } else if (strncmp(p, "ondemand/", 9) == 0) {
            /* app = ondemand/foobar, only pass app=ondemand */
            applen = 8;
            appnamelen = 8;
        } else { /* app!=ondemand, so app is app[/appinstance] */
            if (slash3)
                appnamelen = slash3 - p;
            else if (slash2)
                appnamelen = slash2 - p;

            applen = appnamelen;
        }

        app->av_val = p;
        app->av_len = applen;
        RTMP_Log(RTMP_LOGDEBUG, "Parsed app     : %.*s", applen, p);

        p += appnamelen;
    }

    if (*p == '/')
        p++;

    if (end - p) {
        AVal av = {p, end - p};
        RTMP_ParsePlaypath(&av, playpath);
    }

    return TRUE;
}

/*
 * Extracts playpath from RTMP URL. playpath is the file part of the
 * URL, i.e. the part that comes after rtmp://host:port/app/
 *
 * Returns the stream name in a format understood by FMS. The name is
 * the playpath part of the URL with formatting depending on the stream
 * type:
 *
 * mp4 streams: prepend "mp4:", remove extension
 * mp3 streams: prepend "mp3:", remove extension
 * flv streams: remove extension
 */
void RTMP_ParsePlaypath(AVal *in, AVal *out) {
    int addMP4 = 0;
    int addMP3 = 0;
    int subExt = 0;
    const char *playpath = in->av_val;
    const char *temp, *q, *ext = NULL;
    const char *ppstart = playpath;
    char *streamname, *destptr, *p;

    int pplen = in->av_len;

    out->av_val = NULL;
    out->av_len = 0;

    if ((*ppstart == '?') &&
            (temp = strstr(ppstart, "slist=")) != 0) {
        ppstart = temp + 6;
        pplen = strlen(ppstart);

        temp = strchr(ppstart, '&');
        if (temp) {
            pplen = temp - ppstart;
        }
    }

    q = strchr(ppstart, '?');
    if (pplen >= 4) {
        if (q)
            ext = q - 4;
        else
            ext = &ppstart[pplen - 4];
        if ((strncmp(ext, ".f4v", 4) == 0) ||
                (strncmp(ext, ".mp4", 4) == 0)) {
            addMP4 = 1;
            subExt = 1;
            /* Only remove .flv from rtmp URL, not slist params */
        } else if ((ppstart == playpath) &&
                (strncmp(ext, ".flv", 4) == 0)) {
            subExt = 1;
        } else if (strncmp(ext, ".mp3", 4) == 0) {
            addMP3 = 1;
            subExt = 1;
        }
    }

    streamname = (char *) malloc((pplen + 4 + 1) * sizeof(char));
    if (!streamname)
        return;

    destptr = streamname;
    if (addMP4) {
        if (strncmp(ppstart, "mp4:", 4)) {
            strcpy(destptr, "mp4:");
            destptr += 4;
        } else {
            subExt = 0;
        }
    } else if (addMP3) {
        if (strncmp(ppstart, "mp3:", 4)) {
            strcpy(destptr, "mp3:");
            destptr += 4;
        } else {
            subExt = 0;
        }
    }

    for (p = (char *) ppstart; pplen > 0;) {
        /* skip extension */
        if (subExt && p == ext) {
            p += 4;
            pplen -= 4;
            continue;
        }
        if (*p == '%') {
            unsigned int c;
            sscanf(p + 1, "%02x", &c);
            *destptr++ = c;
            pplen -= 3;
            p += 3;
        } else {
            *destptr++ = *p++;
            pplen--;
        }
    }
    *destptr = '\0';

    out->av_val = streamname;
    out->av_len = destptr - streamname;
}