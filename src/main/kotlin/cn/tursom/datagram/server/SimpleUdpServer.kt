package cn.tursom.datagram.server

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.net.SocketException
import java.util.concurrent.TimeUnit

class SimpleUdpServer(
    override val port: Int,
    private val packageSize: Int = UdpPackageSize.defaultLen,
    private val exception: Exception.() -> Unit = { printStackTrace() },
    private val handler: SimpleUdpServer.(address: SocketAddress, buffer: ByteArray, size: Int) -> Unit
) : UDPServer {

    private val socket = DatagramSocket(port)

    override fun start() {
        Thread(this, "SUdpServer").start()
    }

    override fun run() {
        val inBuff = ByteArray(packageSize)
        val inPacket = DatagramPacket(inBuff, inBuff.size)
        while (true) {
            try {
                //读取inPacket的数据
                socket.receive(inPacket)
                val address = inPacket.socketAddress
                handler(address, inPacket.data, inPacket.length)
            } catch (e: SocketException) {
                if (e.message == "Socket closed" || e.message == "socket closed") {
                    break
                } else {
                    e.exception()
                }
            } catch (e: Exception) {
                e.exception()
            }
        }
    }

    override fun close() {
        socket.close()
    }

    private fun send(p: DatagramPacket) {
        socket.send(p)
    }

    fun SocketAddress.send(buffer: ByteArray) {
        send(DatagramPacket(buffer, buffer.size, this))
    }

    fun SocketAddress.send(buffer: ByteArray, length: Int) {
        send(DatagramPacket(buffer, length, this))
    }
}