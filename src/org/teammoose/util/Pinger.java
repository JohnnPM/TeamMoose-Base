/*
 * Copyright 2014 jamietech. All rights reserved.
 * https://github.com/jamietech/MinecraftServerPing
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */
package org.teammoose.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import com.google.gson.Gson;

public class Pinger
{

	/**
	 * Fetches a {@link PingReply} for the supplied hostname.
	 * <b>Assumed timeout of 2s and port of 25565.</b>
	 * 
	 * @param hostname
	 *            - a valid String hostname
	 * @return {@link PingReply}
	 * @throws IOException
	 */
	public PingReply getPing(final String hostname) throws IOException
	{
		return this.getPing(new PingOptions().setHostname(hostname));
	}

	/**
	 * Fetches a {@link PingReply} for the supplied options.
	 * 
	 * @param options
	 *            - a filled instance of {@link PingOptions}
	 * @return {@link PingReply}
	 * @throws IOException
	 */
	public PingReply getPing(final PingOptions options)
			throws IOException
	{
		PingUtil.validate(options.getHostname(),
				"Hostname cannot be null.");
		PingUtil.validate(options.getPort(), "Port cannot be null.");

		final Socket socket = new Socket();
		socket.connect(
				new InetSocketAddress(options.getHostname(), options.getPort()),
				options.getTimeout());

		final DataInputStream in = new DataInputStream(socket.getInputStream());
		final DataOutputStream out = new DataOutputStream(
				socket.getOutputStream());

		// > Handshake

		ByteArrayOutputStream handshake_bytes = new ByteArrayOutputStream();
		DataOutputStream handshake = new DataOutputStream(handshake_bytes);

		handshake.writeByte(PingUtil.PACKET_HANDSHAKE);
		PingUtil.writeVarInt(handshake,
				PingUtil.PROTOCOL_VERSION);
		PingUtil
				.writeVarInt(handshake, options.getHostname().length());
		handshake.writeBytes(options.getHostname());
		handshake.writeShort(options.getPort());
		PingUtil.writeVarInt(handshake,
				PingUtil.STATUS_HANDSHAKE);

		PingUtil.writeVarInt(out, handshake_bytes.size());
		out.write(handshake_bytes.toByteArray());

		// > Status request

		out.writeByte(0x01); // Size of packet
		out.writeByte(PingUtil.PACKET_STATUSREQUEST);

		// < Status response

		PingUtil.readVarInt(in); // Size
		int id = PingUtil.readVarInt(in);

		PingUtil.io(id == -1, "Server prematurely ended stream.");
		PingUtil.io(id != PingUtil.PACKET_STATUSREQUEST,
				"Server returned invalid packet.");

		int length = PingUtil.readVarInt(in);
		PingUtil.io(length == -1, "Server prematurely ended stream.");
		PingUtil.io(length == 0, "Server returned unexpected value.");

		byte[] data = new byte[length];
		in.readFully(data);
		String json = new String(data, options.getCharset());

		// > Ping

		out.writeByte(0x09); // Size of packet
		out.writeByte(PingUtil.PACKET_PING);
		out.writeLong(System.currentTimeMillis());

		// < Ping

		PingUtil.readVarInt(in); // Size
		id = PingUtil.readVarInt(in);
		PingUtil.io(id == -1, "Server prematurely ended stream.");
		PingUtil.io(id != PingUtil.PACKET_PING,
				"Server returned invalid packet.");

		// Close

		handshake.close();
		handshake_bytes.close();
		out.close();
		in.close();
		socket.close();

		return new Gson().fromJson(json, PingReply.class);
	}

	/**
	 * Storage class for {@link MinecraftPing} options.
	 */
	public class PingOptions
	{

		private String hostname;
		private int port = 25565;
		private int timeout = 2000;
		private String charset = "UTF-8";

		public PingOptions setHostname(String hostname)
		{
			this.hostname = hostname;
			return this;
		}

		public PingOptions setPort(int port)
		{
			this.port = port;
			return this;
		}

		public PingOptions setTimeout(int timeout)
		{
			this.timeout = timeout;
			return this;
		}

		public PingOptions setCharset(String charset)
		{
			this.charset = charset;
			return this;
		}

		public String getHostname()
		{
			return this.hostname;
		}

		public int getPort()
		{
			return this.port;
		}

		public int getTimeout()
		{
			return this.timeout;
		}

		public String getCharset()
		{
			return this.charset;
		}

	}

	/**
	 * References: http://wiki.vg/Server_List_Ping
	 * https://gist.github.com/thinkofdeath/6927216
	 */
	public class PingReply
	{

		private String description;
		private Players players;
		private Version version;
		private String favicon;

		/**
		 * @return the MOTD
		 */
		public String getDescription()
		{
			return this.description;
		}

		/**
		 * @return @{link Players}
		 */
		public Players getPlayers()
		{
			return this.players;
		}

		/**
		 * @return @{link Version}
		 */
		public Version getVersion()
		{
			return this.version;
		}

		/**
		 * @return Base64 encoded favicon image
		 */
		public String getFavicon()
		{
			return this.favicon;
		}

		public class Players
		{
			private int max;
			private int online;
			private List<Player> sample;

			/**
			 * @return Maximum player count
			 */
			public int getMax()
			{
				return this.max;
			}

			/**
			 * @return Online player count
			 */
			public int getOnline()
			{
				return this.online;
			}

			/**
			 * @return List of some players (if any) specified by server
			 */
			public List<Player> getSample()
			{
				return this.sample;
			}
		}

		public class Player
		{
			private String name;
			private String id;

			/**
			 * @return Name of player
			 */
			public String getName()
			{
				return this.name;
			}

			/**
			 * @return Unknown
			 */
			public String getId()
			{
				return this.id;
			}

		}

		public class Version
		{
			private String name;
			private int protocol;

			/**
			 * @return Version name (ex: 13w41a)
			 */
			public String getName()
			{
				return this.name;
			}

			/**
			 * @return Protocol version
			 */
			public int getProtocol()
			{
				return this.protocol;
			}
		}

	}

	public static class PingUtil
	{

		public static byte PACKET_HANDSHAKE = 0x00,
				PACKET_STATUSREQUEST = 0x00, PACKET_PING = 0x01;
		public static int PROTOCOL_VERSION = 4;
		public static int STATUS_HANDSHAKE = 1;

		public static void validate(final Object o, final String m)
		{
			if (o == null)
			{
				throw new RuntimeException(m);
			}
		}

		public static void io(final boolean b, final String m)
				throws IOException
		{
			if (b)
			{
				throw new IOException(m);
			}
		}

		/**
		 * @author thinkofdeath See:
		 *         https://gist.github.com/thinkofdeath/e975ddee04e9c87faf22
		 */
		public static int readVarInt(DataInputStream in) throws IOException
		{
			int i = 0;
			int j = 0;
			while (true)
			{
				int k = in.readByte();

				i |= (k & 0x7F) << j++ * 7;

				if (j > 5)
					throw new RuntimeException("VarInt too big");

				if ((k & 0x80) != 128)
					break;
			}

			return i;
		}

		/**
		 * @author thinkofdeath See:
		 *         https://gist.github.com/thinkofdeath/e975ddee04e9c87faf22
		 * @throws IOException
		 */
		public static void writeVarInt(DataOutputStream out, int paramInt)
				throws IOException
		{
			while (true)
			{
				if ((paramInt & 0xFFFFFF80) == 0)
				{
					out.writeByte(paramInt);
					return;
				}

				out.writeByte(paramInt & 0x7F | 0x80);
				paramInt >>>= 7;
			}
		}

	}
}
