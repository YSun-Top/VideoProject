//
// Created by Void on 2023/5/5.
//

#include <cstdlib>

#include "rtmp_sys.h"
#include "rtmp.h"
#ifndef _WIN32
static int clk_tck;
#endif

uint32_t
RTMP_GetTime()
{
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

void
RTMPPacket_Free(RTMPPacket *p)
{
    if (p->m_body)
    {
        free(p->m_body - RTMP_MAX_HEADER_SIZE);
        p->m_body = NULL;
    }
}