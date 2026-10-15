package com.watchdoc.prototype.data

object SampleDocument {
    const val title = "Computer Networks"
    val text = """
        Computer Networks

        A computer network connects devices so they can exchange data. A local area network covers a home, office, or campus. A wide area network connects sites over larger distances. The Internet is a network of networks that uses shared protocols.

        Layers make network design easier to understand. The application layer describes services such as web browsing and email. The transport layer manages communication between programs. The network layer routes packets. The link layer moves frames across a local connection.

        An IP address identifies an interface for routing. Routers inspect destination addresses and choose the next hop. A packet may cross many networks before it reaches its destination. Routing tables guide this process, and routes can change when links fail.

        TCP provides a reliable byte stream. It numbers data, acknowledges receipt, retransmits missing bytes, and controls how much data is in flight. Reliability does not mean immediate delivery: congestion and packet loss can introduce delays.

        UDP sends independent datagrams with less transport overhead. Applications decide how to handle missing, duplicated, or reordered datagrams. Voice calls, games, and some name lookups use UDP because a late message may be less useful than a timely one.

        DNS translates names into records such as IP addresses. A resolver can cache an answer until its time to live expires. Names and addresses are different concepts: one name can refer to several addresses, and one server can host several named services.

        HTTP defines requests and responses for web resources. HTTPS protects HTTP traffic with TLS. Encryption helps protect content in transit, while certificate validation helps a client authenticate the server it intended to contact.

        Bandwidth describes a link's capacity. Latency describes delay. Throughput describes the rate actually achieved. A fast link can still feel slow when latency is high, and a low-latency connection can still struggle with a large file if throughput is limited.

        Troubleshooting begins with a clear observation. Check the physical or wireless link, the local address, the route, name resolution, and the application response. Change one variable at a time and record what happened. A successful local action alone does not prove that a remote device received the data.

        End of Computer Networks sample. This text was written for the WatchDoc prototype. Use phone-controlled sections to measure the watch's actual reading behavior.
    """.trimIndent()
}
