package cn.tursom.utils.bytebuffer

import java.nio.ByteBuffer

class NioAdvanceByteBuffer(val buffer: ByteBuffer) :
	AdvanceByteBuffer by if (buffer.hasArray()) {
		HeapNioAdvanceByteBuffer(buffer)
	} else {
		DirectNioAdvanceByteBuffer(buffer)
	}
