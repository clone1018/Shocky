package pl.shockah.shocky;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import pl.shockah.Helper;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WebServer {
	private static HttpServer server;
	private static AtomicInteger redirectID = new AtomicInteger(1);
	private static AtomicInteger pasteID = new AtomicInteger(1);
	
	public static boolean start(String host, int port) throws IOException {
		try {
			InetSocketAddress addr = new InetSocketAddress(host, port);
			if (server != null)
				server.stop(0);
			server = HttpServer.create(addr, 0);
			server.start();
			Class.forName("pl.shockah.shocky.WebServer$RedirectHandler");
			Class.forName("pl.shockah.shocky.WebServer$PasteHandler");
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void stop() {
		if (server == null)
			return;
		server.stop(0);
		server = null;
	}
	
	public static boolean exists() {
		return (server != null);
	}
	
	public static InetSocketAddress address() {
		if (server == null)
			return null;
		return server.getAddress();
	}
	
	public static boolean removeContext(HttpContext context) {
		if (server == null)
			return false;
		server.removeContext(context);
		return true;
	}
	
	public static HttpContext addRedirect(String url) {
		if (server == null)
			return null;
		return server.createContext("/s/" + Integer.toString(redirectID.getAndIncrement(), 36), new RedirectHandler(url));
	}
	
	public static HttpContext addPaste(File file) {
		if (server == null)
			return null;
		return server.createContext("/p/" + Integer.toString(pasteID.getAndIncrement(), 36), new PasteHandler(file));
	}
	
	private static class RedirectHandler implements HttpHandler {
		public final String url;

		public RedirectHandler(String url) {
			this.url = url;
		}

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			Headers headers = httpExchange.getResponseHeaders();
			headers.add("Content-Type", "text/plain; charset=utf-8");
			headers.add("Cache-Control", "private; max-age=90");
			headers.add("Location", url);
            byte[] out = url.getBytes(Helper.utf8);
            httpExchange.sendResponseHeaders(301, out.length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(out);
            os.close();
		}
	}
	
	private static class PasteHandler implements HttpHandler {
		public final File file;

		public PasteHandler(File file) {
			this.file = file;
		}

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			Headers headers = httpExchange.getResponseHeaders();
			headers.add("Content-Type", "text/plain; charset=utf-8");
			headers.add("Cache-Control", "private; max-age=90");
			
			byte[] buffer;
			if (!(file.exists() && file.canRead())) {
				buffer = "Paste not found.".getBytes(Helper.utf8);
				httpExchange.sendResponseHeaders(404, buffer.length);
				OutputStream os = httpExchange.getResponseBody();
				try {
					os.write(buffer, 0, buffer.length);
				} finally {
					os.close();
				}
				return;
			}
			httpExchange.sendResponseHeaders(200, file.length());
			
			OutputStream os = httpExchange.getResponseBody();
			InputStream is = new FileInputStream(file);
			try {
				buffer = new byte[1024];
				int count;
				while ((count = is.read(buffer,0,buffer.length)) > 0)
					os.write(buffer, 0, count);
			} finally {
				os.close();
				is.close();
			}
		}
	}
}
