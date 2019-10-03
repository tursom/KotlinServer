package cn.tursom.socket.enhance.impl

import cn.tursom.socket.IAsyncNioSocket
import cn.tursom.socket.enhance.EnhanceSocket
import cn.tursom.socket.enhance.SocketReader
import cn.tursom.socket.enhance.SocketWriter
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer

class StringSocket(
	socket: IAsyncNioSocket,
	prevReader: SocketReader<AdvanceByteBuffer> = LengthFieldBasedFrameReader(socket),
	prevWriter: SocketWriter<AdvanceByteBuffer> = LengthFieldPrependerWriter(socket)
) : EnhanceSocket<String, String> by UnionEnhanceSocket(
	StringReader(prevReader),
	StringWriter(prevWriter),
	socket
)