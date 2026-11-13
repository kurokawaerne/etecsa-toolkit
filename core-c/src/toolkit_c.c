#include "toolkit_c.h"

#include <math.h>
#include <stdio.h>

double tk_rate_bps(const tk_byte_sample *samples, size_t count) {
    if (samples == NULL || count < 2) {
        return 0.0;
    }
    const tk_byte_sample *first = &samples[0];
    const tk_byte_sample *last = &samples[count - 1];
    double dt = last->t_seconds - first->t_seconds;
    if (dt <= 0.0 || last->bytes < first->bytes) {
        return 0.0;
    }
    return (double)(last->bytes - first->bytes) * 8.0 / dt;
}

double tk_ping_mean(const double *pings_ms, size_t count) {
    if (pings_ms == NULL || count == 0) {
        return 0.0;
    }
    double sum = 0.0;
    for (size_t i = 0; i < count; i++) {
        sum += pings_ms[i];
    }
    return sum / (double)count;
}

double tk_ping_jitter(const double *pings_ms, size_t count) {
    if (pings_ms == NULL || count < 2) {
        return 0.0;
    }
    double sum = 0.0;
    for (size_t i = 1; i < count; i++) {
        sum += fabs(pings_ms[i] - pings_ms[i - 1]);
    }
    return sum / (double)(count - 1);
}

int tk_speed_stats(const tk_byte_sample *down, size_t down_count,
                   const tk_byte_sample *up, size_t up_count,
                   const double *pings_ms, size_t ping_count,
                   tk_speed_stats_result *out) {
    if (out == NULL) {
        return -1;
    }
    out->download_bps = tk_rate_bps(down, down_count);
    out->upload_bps = tk_rate_bps(up, up_count);
    out->ping_mean_ms = tk_ping_mean(pings_ms, ping_count);
    out->jitter_ms = tk_ping_jitter(pings_ms, ping_count);
    return 0;
}

int tk_format_rate(double bps, char *buf, size_t buf_size) {
    static const char *const units[] = {"bps", "Kbps", "Mbps", "Gbps", "Tbps"};
    if (bps < 0.0) {
        bps = 0.0;
    }
    size_t unit = 0;
    double value = bps;
    while (value >= 1000.0 && unit + 1 < sizeof(units) / sizeof(units[0])) {
        value /= 1000.0;
        unit++;
    }
    return snprintf(buf, buf_size, "%.2f %s", value, units[unit]);
}
