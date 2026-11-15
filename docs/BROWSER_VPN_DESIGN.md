# Browser-only encrypted access: design and implementation gates

## User requirements
- Personal use only; no subscriptions, paid fallback, ads, or browsing-history monetization.
- Up to four independently hosted endpoints.
- Measure latency, throughput, packet loss, and reachability; select the best usable endpoint.
- Fail over when the active endpoint fails or becomes materially slower. Apply hysteresis to avoid rapid switching.
- Stop browser transport when the Browser is actually closed, not when navigating between app tabs.
- Never claim free hosting until provider terms, quotas, egress charges, and account limits are verified.

## Verified Android platform constraints
- Android `VpnService.Builder.addAllowedApplication()` and `addDisallowedApplication()` filter by installed package, not by Activity, WebView, or ExoPlayer component. A normal per-app VPN therefore cannot isolate WebView from ExoPlayer inside this same application.
- AndroidX WebKit `ProxyController` applies a process-specific proxy override to WebView-initiated requests and explicitly documents that it governs WebView requests. It is not a VPN and applies to all WebViews in this app process.
- A browser-only encrypted proxy is the safer first implementation boundary. It must not be marketed as a device VPN. A true WireGuard/VpnService implementation is blocked until a device PoC proves that WebView traffic is tunneled while ExoPlayer and all unrelated app traffic remain outside it, including DNS, IPv4/IPv6, TCP, UDP/QUIC, redirects, and reconnects.

## Endpoint pool contract
- Configure zero to four endpoints. Empty configuration means feature unavailable, not a fake connected state.
- Endpoint IDs are non-secret stable identifiers. Private keys, credentials, and endpoint tokens must never be committed or logged.
- Measure each endpoint from the device. Do not infer speed from geographic distance.
- Reject unreachable endpoints, endpoints with >=20% packet loss, and incomplete measurements.
- Rank usable endpoints by packet loss, then download throughput, then latency. Retain the active endpoint unless latency or throughput improves by the configured threshold, or packet loss improves by at least 10 percentage points.
- Switch immediately if current endpoint is unusable. Otherwise switch only for a material improvement (default 20%) to prevent oscillation.
- If all endpoints fail, stop the tunnel/proxy and show an explicit disconnected state. Never silently fall back to direct access if the user has enabled a privacy lock mode.

## Free-only hosting gate
No server addresses are included in source code. Before adding any endpoint, verify in writing:
1. $0 recurring cost and $0 egress/transfer charges for the expected personal traffic.
2. No automatic paid upgrade or billing activation.
3. Terms permit private VPN/proxy traffic.
4. Region, bandwidth quota, idle suspension, uptime, and account limits.
5. Privacy/logging policy and ability to rotate/revoke keys.

A free software license does not provide free server capacity. Do not deploy or enable a paid resource without explicit user approval.

## Current implementation status
- Pure Kotlin four-endpoint selection policy and unit tests are implemented in the domain module.
- Android WebView proxy wiring, tunnel protocol integration, endpoint provisioning, device-level traffic isolation, DNS/QUIC leak tests, and real-world failover measurements are not yet implemented.
- No server endpoint is currently configured. This is intentional; do not represent the feature as connected or production-ready.
