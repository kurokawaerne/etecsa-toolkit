/* Standalone test for toolkit_c: asserts known values and prints
 * PASS. Run via ctest or directly: ./toolkit_c_test */
#include <math.h>
#include <stdio.h>
#include <string.h>

#include "toolkit_c.h"

static int failures = 0;

static void expect_close(const char *what, double got, double want) {
    if (fabs(got - want) > 1e-6) {
        printf("FAIL %s: got %.6f, want %.6f\n", what, got, want);
        failures++;
    }
}

static void expect_str(const char *what, const char *got, const char *want) {
    if (strcmp(got, want) != 0) {
        printf("FAIL %s: got \"%s\", want \"%s\"\n", what, got, want);
        failures++;
    }
}

int main(void) {
    /* Download: 3,000,000 bytes in 2 s = 12,000,000 bps.
     * Upload:   500,000 bytes in 2 s =  2,000,000 bps. */
    const tk_byte_sample down[] = {{0.0, 0}, {1.0, 1500000}, {2.0, 3000000}};
    const tk_byte_sample up[] = {{0.0, 0}, {2.0, 500000}};
    const double pings[] = {10.0, 14.0, 11.0}; /* mean 35/3, jitter (4+3)/2 */

    tk_speed_stats_result st;
    if (tk_speed_stats(down, 3, up, 2, pings, 3, &st) != 0) {
        printf("FAIL tk_speed_stats return code\n");
        return 1;
    }
    expect_close("download_bps", st.download_bps, 12000000.0);
    expect_close("upload_bps", st.upload_bps, 2000000.0);
    expect_close("ping_mean_ms", st.ping_mean_ms, 35.0 / 3.0);
    expect_close("jitter_ms", st.jitter_ms, 3.5);

    /* Degenerate inputs degrade to 0, never NaN/garbage. */
    expect_close("rate of one sample", tk_rate_bps(down, 1), 0.0);
    expect_close("rate of NULL", tk_rate_bps(NULL, 0), 0.0);
    expect_close("mean of none", tk_ping_mean(NULL, 0), 0.0);
    expect_close("jitter of one", tk_ping_jitter(pings, 1), 0.0);
    if (tk_speed_stats(down, 3, up, 2, pings, 3, NULL) != -1) {
        printf("FAIL tk_speed_stats NULL out should return -1\n");
        failures++;
    }

    /* format_rate across the unit ladder. */
    char buf[64];
    tk_format_rate(500.0, buf, sizeof(buf));
    expect_str("format bps", buf, "500.00 bps");
    tk_format_rate(1500.0, buf, sizeof(buf));
    expect_str("format Kbps", buf, "1.50 Kbps");
    tk_format_rate(1240000.0, buf, sizeof(buf));
    expect_str("format Mbps", buf, "1.24 Mbps");
    tk_format_rate(12000000.0, buf, sizeof(buf));
    expect_str("format 12 Mbps", buf, "12.00 Mbps");
    tk_format_rate(2500000000.0, buf, sizeof(buf));
    expect_str("format Gbps", buf, "2.50 Gbps");
    tk_format_rate(-5.0, buf, sizeof(buf));
    expect_str("format negative clamps", buf, "0.00 bps");

    if (failures > 0) {
        printf("%d FAILURE(S)\n", failures);
        return 1;
    }
    printf("PASS\n");
    return 0;
}
