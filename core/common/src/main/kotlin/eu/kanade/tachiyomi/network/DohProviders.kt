package eu.kanade.tachiyomi.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.Inet4Address

/**
 * Based on https://github.com/square/okhttp/blob/ef5d0c83f7bbd3a0c0534e7ca23cbc4ee7550f3b/okhttp-dnsoverhttps/src/test/java/okhttp3/dnsoverhttps/DohProviders.java
 */

const val PREF_DOH_CLOUDFLARE = 1
const val PREF_DOH_GOOGLE = 2
const val PREF_DOH_ADGUARD = 3
const val PREF_DOH_QUAD9 = 4
const val PREF_DOH_ALIDNS = 5
const val PREF_DOH_DNSPOD = 6
const val PREF_DOH_360 = 7
const val PREF_DOH_QUAD101 = 8
const val PREF_DOH_MULLVAD = 9
const val PREF_DOH_CONTROLD = 10
const val PREF_DOH_NJALLA = 11
const val PREF_DOH_SHECAN = 12
const val PREF_DOH_LIBREDNS = 13

/**
 * Custom DNS yang hanya mengembalikan alamat IPv4
 */
private class IPv4OnlyDns(private val delegate: DnsOverHttps) : okhttp3.Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        // Filter hanya alamat IPv4 dari hasil lookup
        return delegate.lookup(hostname).filterIsInstance<Inet4Address>()
    }
}

/**
 * Helper function untuk membuat DnsOverHttps dengan preferensi IPv4
 */
private fun OkHttpClient.Builder.buildDohWithIPv4Preference(
    url: String,
    vararg bootstrapDns: InetAddress
): okhttp3.Dns {
    // Filter bootstrap DNS untuk hanya menggunakan IPv4
    val ipv4BootstrapDns = bootstrapDns.filterIsInstance<Inet4Address>()
    
    val doh = DnsOverHttps.Builder()
        .client(build())
        .url(url.toHttpUrl())
        .apply {
            if (ipv4BootstrapDns.isNotEmpty()) {
                bootstrapDnsHosts(*ipv4BootstrapDns.toTypedArray())
            }
        }
        .build()
    
    // Bungkus dengan IPv4OnlyDns untuk memastikan hanya IPv4 yang dikembalikan
    return IPv4OnlyDns(doh)
}

fun OkHttpClient.Builder.dohCloudflare() = dns(
    buildDohWithIPv4Preference(
        "https://cloudflare-dns.com/dns-query",
        InetAddress.getByName("162.159.36.1"),
        InetAddress.getByName("162.159.46.1"),
        InetAddress.getByName("1.1.1.1"),
        InetAddress.getByName("1.0.0.1"),
        InetAddress.getByName("162.159.132.53")
        // IPv6 addresses removed for IPv4 preference
    )
)

fun OkHttpClient.Builder.dohGoogle() = dns(
    buildDohWithIPv4Preference(
        "https://dns.google/dns-query",
        InetAddress.getByName("8.8.4.4"),
        InetAddress.getByName("8.8.8.8")
        // IPv6 addresses removed for IPv4 preference
    )
)

fun OkHttpClient.Builder.dohAdGuard() = dns(
    buildDohWithIPv4Preference(
        "https://dns-unfiltered.adguard.com/dns-query",
        InetAddress.getByName("94.140.14.140"),
        InetAddress.getByName("94.140.14.141")
        // IPv6 addresses removed for IPv4 preference
    )
)

fun OkHttpClient.Builder.dohQuad9() = dns(
    buildDohWithIPv4Preference(
        "https://dns.quad9.net/dns-query",
        InetAddress.getByName("9.9.9.9"),
        InetAddress.getByName("149.112.112.112")
        // IPv6 addresses removed for IPv4 preference
    )
)

fun OkHttpClient.Builder.dohAliDNS() = dns(
    buildDohWithIPv4Preference(
        "https://dns.alidns.com/dns-query",
        InetAddress.getByName("223.5.5.5"),
        InetAddress.getByName("223.6.6.6")
        // IPv6 addresses removed for IPv4 preference
    )
)

fun OkHttpClient.Builder.dohDNSPod() = dns(
    buildDohWithIPv4Preference(
        "https://doh.pub/dns-query",
        InetAddress.getByName("1.12.12.12"),
        InetAddress.getByName("120.53.53.53")
    )
)

fun OkHttpClient.Builder.doh360() = dns(
    buildDohWithIPv4Preference(
        "https://doh.360.cn/dns-query",
        InetAddress.getByName("101.226.4.6"),
        InetAddress.getByName("218.30.118.6"),
        InetAddress.getByName("123.125.81.6"),
        InetAddress.getByName("140.207.198.6"),
        InetAddress.getByName("180.163.249.75"),
        InetAddress.getByName("101.199.113.208"),
        InetAddress.getByName("36.99.170.86")
    )
)

fun OkHttpClient.Builder.dohQuad101() = dns(
    buildDohWithIPv4Preference(
        "https://dns.twnic.tw/dns-query",
        InetAddress.getByName("101.101.101.101")
        // IPv6 addresses removed for IPv4 preference
    )
)

fun OkHttpClient.Builder.dohMullvad() = dns(
    buildDohWithIPv4Preference(
        "https://dns.mullvad.net/dns-query", // Fixed: removed leading space
        InetAddress.getByName("194.242.2.2")
        // IPv6 address removed for IPv4 preference
    )
)

fun OkHttpClient.Builder.dohControlD() = dns(
    buildDohWithIPv4Preference(
        "https://freedns.controld.com/p0",
        InetAddress.getByName("76.76.2.0"),
        InetAddress.getByName("76.76.10.0")
        // IPv6 addresses removed for IPv4 preference
    )
)

// Fixed: Changed function name from dohNajalla to dohNjalla to match constant
fun OkHttpClient.Builder.dohNjalla() = dns(
    buildDohWithIPv4Preference(
        "https://dns.njal.la/dns-query",
        InetAddress.getByName("95.215.19.53")
        // IPv6 address removed for IPv4 preference
    )
)

fun OkHttpClient.Builder.dohShecan() = dns(
    buildDohWithIPv4Preference(
        "https://free.shecan.ir/dns-query",
        InetAddress.getByName("178.22.122.100"),
        InetAddress.getByName("185.51.200.2")
    )
)

fun OkHttpClient.Builder.dohLibreDNS() = dns(
    buildDohWithIPv4Preference(
        "https://doh.libredns.gr/dns-query",
        InetAddress.getByName("116.202.176.26")
        // IPv6 address removed for IPv4 preference
    )
)