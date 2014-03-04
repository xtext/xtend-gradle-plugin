package org.xtend.gradle.tasks;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.martiansoftware.nailgun.NGConstants;

public class NailgunClient {

	public static NailgunClient onLocalHost(int port) {
		try {
			return new NailgunClient(InetAddress.getLocalHost(), port);
		} catch (UnknownHostException e) {
			throw Throwables.propagate(e);
		}
	}

	private int port;
	private InetAddress address;
	private Charset encoding = Charsets.UTF_8;

	public NailgunClient(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	public boolean serverAvailable() {
		return run("ng-version") == 0;
	}

	public int run(String command) {
		try {
			ByteArrayOutputStream dummyOut = new ByteArrayOutputStream();
			return send(command, new ArrayList<String>(), new File(""), dummyOut, dummyOut);
		} catch (Exception e) {
			return -1;
		}
	}

	public int send(String command, List<String> args, File cwd, OutputStream out, OutputStream err) {
		try (Socket socket = new Socket(address, port);
				OutputStream socketOut = socket.getOutputStream();
				DataInputStream socketIn = new DataInputStream(socket.getInputStream());) {
			sendCommand(command, args, cwd, socketOut);
			return receiveOutput(socketIn, out, err);
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	private void sendCommand(String command, List<String> args, File cwd, OutputStream out) throws IOException {
		for (String arg : args) {
			putChunk(NGConstants.CHUNKTYPE_ARGUMENT, arg, out);
		}
		putChunk(NGConstants.CHUNKTYPE_WORKINGDIRECTORY, cwd.getCanonicalPath(), out);
		putChunk(NGConstants.CHUNKTYPE_COMMAND, command, out);
	}

	private void putChunk(byte chunkType, String data, OutputStream output) throws IOException {
		output.write(createHeader(data.length(), chunkType));
		output.write(data.getBytes(encoding));
	}

	private byte[] createHeader(int size, byte chunkType) {
		return ByteBuffer.allocate(5).putInt(size).put(chunkType).array();
	}

	private int receiveOutput(DataInputStream in, OutputStream out, OutputStream err) throws IOException {
		Integer exitCode = null;
		try {
			Chunk chunk = getChunk(in);
			if (chunk.getChunkType() == NGConstants.CHUNKTYPE_EXIT) {
				exitCode = Integer.valueOf(new String(chunk.getData(), encoding));
			} else if (chunk.getChunkType() == NGConstants.CHUNKTYPE_STDOUT) {
				out.write(chunk.getData());
			} else if (chunk.getChunkType() == NGConstants.CHUNKTYPE_STDERR) {
				err.write(chunk.getData());
			}
		} catch (Exception e) {
			exitCode = 897;
		}
		return exitCode == null ? receiveOutput(in, out, err) : exitCode;
	}

	private Chunk getChunk(DataInputStream input) throws IOException {
		byte[] rawHeader = new byte[5];
		input.readFully(rawHeader);
		Header header = readHeader(rawHeader);
		byte[] data = new byte[header.getSize()];
		input.readFully(data);
		return new Chunk(header.getChunkType(), data);
	}

	private Header readHeader(byte[] rawHeader) {
		ByteBuffer buffer = ByteBuffer.wrap(rawHeader, 0, 5);
		return new Header(buffer.getInt(), buffer.get());
	}

	private class Chunk {
		private byte chunkType;
		private byte[] data;

		public Chunk(byte chunkType, byte[] data) {
			this.chunkType = chunkType;
			this.data = Arrays.copyOf(data, data.length);
		}

		public byte getChunkType() {
			return chunkType;
		}

		public byte[] getData() {
			return Arrays.copyOf(data, data.length);
		}
	}

	private class Header {
		private int size;
		private byte chunkType;

		public Header(int size, byte chunkType) {
			this.size = size;
			this.chunkType = chunkType;
		}

		public int getSize() {
			return size;
		}

		public byte getChunkType() {
			return chunkType;
		}
	}

}
