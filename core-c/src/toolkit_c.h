/* toolkit_c — C11 native core for ETECSA Toolkit.
 *
 * Speed-test statistics: turns timestamped cumulative byte counters
 * (as sampled by the app while a transfer runs) into bit rates, and
 * ping samples into mean / jitter. Pure functions, no globals, no
 * allocation — safe to call from JNI on any thread.
 */
#ifndef TOOLKIT_C_H
#define TOOLKIT_C_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* One counter sample: at `t_seconds` (monotonic clock) the transfer
 * had moved `bytes` bytes in total. */
typedef struct {
    double t_seconds;
    uint64_t bytes;
} tk_byte_sample;

/* Computed statistics. Rates are bits per second; ping and jitter
 * are milliseconds. A field is 0 when it cannot be computed (too
 * few samples, zero elapsed time). */
typedef struct {
    double download_bps;
    double upload_bps;
    double ping_mean_ms;
    double jitter_ms;
} tk_speed_stats_result;

/* Mean rate in bits/second between the first and last sample of a
 * cumulative counter series. 0.0 for <2 samples or no elapsed time. */
double tk_rate_bps(const tk_byte_sample *samples, size_t count);

/* Mean of `count` ping samples (ms); 0.0 when count == 0. */
double tk_ping_mean(const double *pings_ms, size_t count);

/* Jitter: mean absolute difference between successive ping samples
 * (ms); 0.0 when fewer than 2 samples. */
double tk_ping_jitter(const double *pings_ms, size_t count);

/* Fill `out` from download/upload counter series and ping samples.
 * Any series may be NULL with count 0. Returns 0 on success, -1 if
 * `out` is NULL. */
int tk_speed_stats(const tk_byte_sample *down, size_t down_count,
                   const tk_byte_sample *up, size_t up_count,
                   const double *pings_ms, size_t ping_count,
                   tk_speed_stats_result *out);

/* Human-readable rate: "500.00 bps", "1.50 Kbps", "12.34 Mbps",
 * "2.50 Gbps" (decimal units). Writes into `buf` (truncated safely,
 * always NUL-terminated when buf_size > 0) and returns the number of
 * characters that would have been written, like snprintf. Negative
 * rates are clamped to 0. */
int tk_format_rate(double bps, char *buf, size_t buf_size);

#ifdef __cplusplus
}
#endif

#endif /* TOOLKIT_C_H */
