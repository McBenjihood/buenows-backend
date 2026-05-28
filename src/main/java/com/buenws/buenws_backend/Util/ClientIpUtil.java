package com.buenws.buenws_backend.Util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

public final class ClientIpUtil {
    private static final String DEFAULT_TRUSTED_PROXIES = "loopback,private";
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6_LITERAL_PATTERN = Pattern.compile("^[0-9a-fA-F:.]+$");

    private ClientIpUtil() {}

    public static String getClientIp(HttpServletRequest request) {
        return getClientIp(request, defaultTrustedProxyCidrs());
    }

    public static String getClientIp(HttpServletRequest request, String trustedProxyCidrs) {
        if (request == null) return "unknown";

        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (isTrustedProxy(remoteAddr, trustedProxyCidrs)) {
            String forwardedFor = firstForwardedIp(request.getHeader("X-Forwarded-For"));
            if (!forwardedFor.isBlank()) return forwardedFor;

            String realIp = normalizeIp(request.getHeader("X-Real-IP"));
            if (!realIp.isBlank()) return realIp;
        }

        return remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }

    public static String defaultTrustedProxyCidrs() {
        String systemProperty = System.getProperty("app.trusted-proxy-cidrs");
        if (systemProperty != null && !systemProperty.isBlank()) return systemProperty;

        String appEnv = System.getenv("APP_TRUSTED_PROXY_CIDRS");
        if (appEnv != null && !appEnv.isBlank()) return appEnv;

        String legacyEnv = System.getenv("TRUSTED_PROXY_CIDRS");
        return legacyEnv == null || legacyEnv.isBlank() ? DEFAULT_TRUSTED_PROXIES : legacyEnv;
    }

    private static String firstForwardedIp(String value) {
        if (value == null || value.isBlank()) return "";
        for (String part : value.split(",")) {
            String ip = normalizeIp(part);
            if (!ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) return ip;
        }
        return "";
    }

    private static String normalizeIp(String value) {
        if (value == null) return "";
        String candidate = value.trim();
        if (candidate.isBlank() || "unknown".equalsIgnoreCase(candidate)) return "";

        if (candidate.startsWith("[") && candidate.contains("]")) {
            candidate = candidate.substring(1, candidate.indexOf(']')).trim();
        } else {
            int lastColon = candidate.lastIndexOf(':');
            if (lastColon > -1 && candidate.indexOf(':') == lastColon) {
                String possibleIpv4 = candidate.substring(0, lastColon);
                if (IPV4_PATTERN.matcher(possibleIpv4).matches()) candidate = possibleIpv4;
            }
        }

        return isIpLiteral(candidate) ? candidate : "";
    }

    private static boolean isTrustedProxy(String remoteAddr, String trustedProxyCidrs) {
        if (remoteAddr.isBlank() || trustedProxyCidrs == null || trustedProxyCidrs.isBlank()) return false;
        for (String rawToken : trustedProxyCidrs.split(",")) {
            String token = rawToken.trim();
            if (token.isBlank() || "none".equalsIgnoreCase(token)) continue;
            if ("*".equals(token)) return true;
            if ("loopback".equalsIgnoreCase(token) && isLoopback(remoteAddr)) return true;
            if ("private".equalsIgnoreCase(token) && isPrivateAddress(remoteAddr)) return true;
            if (token.contains("/") && ipv4InCidr(remoteAddr, token)) return true;
            if (remoteAddr.equals(token)) return true;
        }
        return false;
    }

    private static boolean isIpLiteral(String value) {
        return IPV4_PATTERN.matcher(value).matches() || (value.contains(":") && IPV6_LITERAL_PATTERN.matcher(value).matches());
    }

    private static boolean isLoopback(String value) {
        if ("::1".equals(value) || "0:0:0:0:0:0:0:1".equals(value)) return true;
        Long ipv4 = ipv4ToLong(value);
        return ipv4 != null && (ipv4 >>> 24) == 127;
    }

    private static boolean isPrivateAddress(String value) {
        String lower = value.toLowerCase();
        if (lower.startsWith("fc") || lower.startsWith("fd") || lower.startsWith("fe80:")) return true;
        Long ipv4 = ipv4ToLong(value);
        if (ipv4 == null) return false;
        return (ipv4 >= ipv4ToLong("10.0.0.0") && ipv4 <= ipv4ToLong("10.255.255.255"))
                || (ipv4 >= ipv4ToLong("172.16.0.0") && ipv4 <= ipv4ToLong("172.31.255.255"))
                || (ipv4 >= ipv4ToLong("192.168.0.0") && ipv4 <= ipv4ToLong("192.168.255.255"));
    }

    private static boolean ipv4InCidr(String value, String cidr) {
        Long ip = ipv4ToLong(value);
        if (ip == null) return false;
        String[] parts = cidr.split("/", 2);
        if (parts.length != 2) return false;
        Long network = ipv4ToLong(parts[0].trim());
        if (network == null) return false;
        try {
            int prefix = Integer.parseInt(parts[1].trim());
            if (prefix < 0 || prefix > 32) return false;
            long mask = prefix == 0 ? 0 : 0xffffffffL << (32 - prefix);
            return (ip & mask) == (network & mask);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static Long ipv4ToLong(String value) {
        if (value == null || !IPV4_PATTERN.matcher(value).matches()) return null;
        String[] parts = value.split("\\.");
        long output = 0;
        for (String part : parts) output = (output << 8) + Integer.parseInt(part);
        return output;
    }
}
