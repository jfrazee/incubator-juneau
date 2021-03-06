// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.client;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.parser.ParserContext.*;
import static org.apache.juneau.serializer.SerializerContext.*;

import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.conn.util.*;
import org.apache.http.cookie.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;

/**
 * Builder class for the {@link RestClient} class.
 */
@SuppressWarnings({"hiding"})
public class RestClientBuilder extends CoreObjectBuilder {

	private HttpClientConnectionManager httpClientConnectionManager;
	private HttpClientBuilder httpClientBuilder = createHttpClientBuilder();
	private CloseableHttpClient httpClient;
	private boolean keepHttpClientOpen;

	private Class<? extends Serializer> serializerClass = JsonSerializer.class;
	private Class<? extends Parser> parserClass = JsonParser.class;
	private Class<? extends PartSerializer> partSerializerClass = UrlEncodingSerializer.class;
	private Serializer serializer;
	private Parser parser;
	private PartSerializer partSerializer;

	private Map<String,String> headers = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);

	private List<RestCallInterceptor> interceptors = new ArrayList<RestCallInterceptor>();

	private String rootUrl;
	private SSLOpts sslOpts;
	private boolean pooled;

	private int retries = 1;
	private long retryInterval = -1;
	private RetryOn retryOn = RetryOn.DEFAULT;
	private boolean debug, executorServiceShutdownOnClose;
	private ExecutorService executorService;

	/**
	 * Constructor, default settings.
	 */
	public RestClientBuilder() {
		super();
	}

	/**
	 * Constructor, default settings.
	 * <p>
	 * Shortcut for calling <code><jk>new</jk> RestClientBuilder().serializer(s).parser(p);</code>
	 *
	 * @param s The serializer to use for output.
	 * @param p The parser to use for input.
	 */
	public RestClientBuilder(Serializer s, Parser p) {
		super();
		serializer(s);
		parser(p);
	}

	/**
	 * Constructor, default settings.
	 * <p>
	 * Shortcut for calling <code><jk>new</jk> RestClientBuilder().serializer(s).parser(p);</code>
	 *
	 * @param s The serializer class to use for output.
	 * @param p The parser class to use for input.
	 */
	public RestClientBuilder(Class<? extends Serializer> s, Class<? extends Parser> p) {
		super();
		serializer(s);
		parser(p);
	}

	/**
	 * Constructor.
	 * @param propertyStore The initial configuration settings for this builder.
	 */
	public RestClientBuilder(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObjectBuilder */
	public RestClient build() {
		try {
			CloseableHttpClient httpClient = this.httpClient;
			if (httpClient == null)
				httpClient = createHttpClient();

			Serializer s =
				this.serializer != null
				? this.serializer.builder().apply(propertyStore).build()
				: new SerializerBuilder(propertyStore).build(this.serializerClass);

			Parser p =
				this.parser != null
				? this.parser.builder().apply(propertyStore).build()
				: new ParserBuilder(propertyStore).build(this.parserClass);

			UrlEncodingSerializer us = new SerializerBuilder(propertyStore).build(UrlEncodingSerializer.class);

			PartSerializer pf = null;
			if (partSerializer != null)
				pf = partSerializer;
			else if (partSerializerClass != null) {
				if (partSerializerClass == UrlEncodingSerializer.class)
					pf = us;
				else
					pf = partSerializerClass.newInstance();
			}

			return new RestClient(propertyStore, httpClient, keepHttpClientOpen, s, p, us, pf, headers, interceptors, rootUrl, retryOn, retries, retryInterval, debug, executorService, executorServiceShutdownOnClose);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates an instance of an {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 * <p>
	 * This HTTP client is used when the HTTP client is not specified through one of the constructors or the
	 * 	{@link #httpClient(CloseableHttpClient, boolean)} method.
	 * <p>
	 * Subclasses can override this method to provide specially-configured HTTP clients to handle
	 * 	stuff such as SSL/TLS certificate handling, authentication, etc.
	 * <p>
	 * The default implementation returns an instance of {@link HttpClient} using the client builder
	 * 	returned by {@link #createHttpClientBuilder()}.
	 *
	 * @return The HTTP client to use.
	 * @throws Exception
	 */
	protected CloseableHttpClient createHttpClient() throws Exception {
		// Don't call createConnectionManager() if RestClient.setConnectionManager() was called.
		if (httpClientConnectionManager == null)
			httpClientBuilder.setConnectionManager(createConnectionManager());
		return httpClientBuilder.build();
	}

	/**
	 * Creates an instance of an {@link HttpClientBuilder} to be used to create
	 * 	the {@link HttpClient}.
	 * <p>
	 * Subclasses can override this method to provide their own client builder.
	 * <p>
	 * The predefined method returns an {@link HttpClientBuilder} with the following settings:
	 * <ul>
	 * 	<li>Lax redirect strategy.
	 * 	<li>The connection manager returned by {@link #createConnectionManager()}.
	 * </ul>
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 */
	protected HttpClientBuilder createHttpClientBuilder() {
		HttpClientBuilder b = HttpClientBuilder.create();
		b.setRedirectStrategy(new AllowAllRedirects());
		return b;
	}

	/**
	 * Creates the {@link HttpClientConnectionManager} returned by {@link #createConnectionManager()}.
	 * <p>
	 * Subclasses can override this method to provide their own connection manager.
	 * <p>
	 * The default implementation returns an instance of a {@link PoolingHttpClientConnectionManager}.
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 */
	protected HttpClientConnectionManager createConnectionManager() {
		if (sslOpts != null) {
			HostnameVerifier hv = null;
			switch (sslOpts.getHostVerify()) {
				case LAX: hv = new NoopHostnameVerifier(); break;
				case DEFAULT: hv = new DefaultHostnameVerifier(); break;
				default: throw new RuntimeException("Programmer error");
			}

			for (String p : split(sslOpts.getProtocols(), ',')) {
				try {
					TrustManager tm = new SimpleX509TrustManager(sslOpts.getCertValidate() == SSLOpts.CertValidate.LAX);

					SSLContext ctx = SSLContext.getInstance(p);
					ctx.init(null, new TrustManager[] { tm }, null);

					// Create a socket to ensure this algorithm is acceptable.
					// This will correctly disallow certain configurations (such as SSL_TLS under FIPS)
					ctx.getSocketFactory().createSocket().close();
					SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(ctx, hv);
					setSSLSocketFactory(sf);

					Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory> create().register("https", sf).build();

					return (pooled ? new PoolingHttpClientConnectionManager(r) : new BasicHttpClientConnectionManager(r));
				} catch (Throwable t) {}
			}
		}

			// Using pooling connection so that this client is threadsafe.
		return (pooled ? new PoolingHttpClientConnectionManager() : new BasicHttpClientConnectionManager());
	}

	/**
	 * Set a root URL for this client.
	 * <p>
	 * When set, URL strings passed in through the various rest call methods (e.g. {@link RestClient#doGet(Object)}
	 * 	will be prefixed with the specified root.
	 * This root URL is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URL string.
	 *
	 * @param rootUrl The root URL to prefix to relative URL strings.  Trailing slashes are trimmed.
	 * Usually a <code>String<code> but you can also pass in <code>URI</code> and <code>URL</code> objects as well.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder rootUrl(Object rootUrl) {
		String s = rootUrl.toString();
		if (s.endsWith("/"))
			s = s.replaceAll("\\/$", "");
		this.rootUrl = s;
		return this;
	}

	/**
	 * Enable SSL support on this client.
	 *
	 * @param opts The SSL configuration options.  See {@link SSLOpts} for details.
	 * This method is a no-op if <code>sslConfig</code> is <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 */
	public RestClientBuilder enableSSL(SSLOpts opts) throws KeyStoreException, NoSuchAlgorithmException {
		this.sslOpts = opts;
		return this;
	}

	/**
	 * Enable LAX SSL support.
	 * <p>
	 * Certificate chain validation and hostname verification is disabled.
	 *
	 * @return This object (for method chaining).
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 */
	public RestClientBuilder enableLaxSSL() throws KeyStoreException, NoSuchAlgorithmException {
		return enableSSL(SSLOpts.LAX);
	}

	/**
	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
	 *
	 * @param version The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder clientVersion(String version) {
		return header("X-Client-Version", version);
	}

	/**
	 * Adds an interceptor that gets called immediately after a connection is made.
	 *
	 * @param interceptor The interceptor.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder interceptor(RestCallInterceptor interceptor) {
		interceptors.add(interceptor);
		return this;
	}

	/**
	 * Adds a {@link RestCallLogger} to the list of interceptors on this class.
	 *
	 * @param level The log level to log messsages at.
	 * @param log The logger to log messages to.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder logTo(Level level, Logger log) {
		interceptor(new RestCallLogger(level, log));
		return this;
	}

	/**
	 * Make HTTP calls retryable if an error response (>=400) is received.
	 *
	 * @param retries The number of retries to attempt.
	 * @param interval The time in milliseconds between attempts.
	 * @param retryOn Optional object used for determining whether a retry should be attempted.
	 * If <jk>null</jk>, uses {@link RetryOn#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder retryable(int retries, long interval, RetryOn retryOn) {
		this.retries = retries;
		this.retryInterval = interval;
		this.retryOn = retryOn;
		return this;
	}

	/**
	 * When called, the {@link #createConnectionManager()} method will return a {@link PoolingHttpClientConnectionManager}
	 * 	instead of a {@link BasicHttpClientConnectionManager}.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder pooled() {
		this.pooled = true;
		return this;
	}

	/**
	 * Sets the serializer used for serializing POJOs to the HTTP request message body.
	 *
	 * @param serializer The serializer.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder serializer(Serializer serializer) {
		this.serializer = serializer;
		return this;
	}

	/**
	 * Same as {@link #serializer(Serializer)}, except takes in a serializer class that
	 * 	will be instantiated through a no-arg constructor.
	 *
	 * @param serializerClass The serializer class.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder serializer(Class<? extends Serializer> serializerClass) {
		this.serializerClass = serializerClass;
		return this;
	}

	/**
	 * Sets the parser used for parsing POJOs from the HTTP response message body.
	 *
	 * @param parser The parser.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder parser(Parser parser) {
		this.parser = parser;
		return this;
	}

	/**
	 * Same as {@link #parser(Parser)}, except takes in a parser class that
	 * 	will be instantiated through a no-arg constructor.
	 *
	 * @param parserClass The parser class.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder parser(Class<? extends Parser> parserClass) {
		this.parserClass = parserClass;
		return this;
	}

	/**
	 * Sets the part serializer to use for converting POJOs to headers, query parameters, form-data parameters, and
	 * path variables.
	 *
	 * @param partSerializer The part serializer instance.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partSerializer(PartSerializer partSerializer) {
		this.partSerializer = partSerializer;
		return this;
	}

	/**
	 * Sets the part formatter to use for converting POJOs to headers, query parameters, form-data parameters, and
	 * path variables.
	 *
	 * @param partSerializerClass The part serializer class.
	 * 	The class must have a no-arg constructor.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partSerializer(Class<? extends PartSerializer> partSerializerClass) {
		this.partSerializerClass = partSerializerClass;
		return this;
	}

	/**
	 * Set up this client to use BASIC auth.
	 *
	 * @param host The auth scope hostname.
	 * @param port The auth scope port.
	 * @param user The username.
	 * @param pw The password.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder basicAuth(String host, int port, String user, String pw) {
		AuthScope scope = new AuthScope(host, port);
		Credentials up = new UsernamePasswordCredentials(user, pw);
		CredentialsProvider p = new BasicCredentialsProvider();
		p.setCredentials(scope, up);
		setDefaultCredentialsProvider(p);
		return this;
	}

	/**
	 * Sets the internal {@link HttpClient} to use for handling HTTP communications.
	 *
	 * @param httpClient The HTTP client.
	 * @param keepHttpClientOpen Don't close this client when the {@link RestClient#close()} method is called.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder httpClient(CloseableHttpClient httpClient, boolean keepHttpClientOpen) {
		this.httpClient = httpClient;
		this.keepHttpClientOpen = keepHttpClientOpen;
		return this;
	}

	/**
	 * Defines the executor service to use when calling future methods on the {@link RestCall} class.
	 * <p>
	 * This executor service is used to create {@link Future} objects on the following methods:
	 * <ul>
	 * 	<li>{@link RestCall#runFuture()}
	 * 	<li>{@link RestCall#getResponseFuture(Class)}
	 * 	<li>{@link RestCall#getResponseFuture(Type,Type...)}
	 * 	<li>{@link RestCall#getResponseAsString()}
	 * </ul>
	 * <p>
	 * The default executor service is a single-threaded {@link ThreadPoolExecutor} with a 30 second timeout
	 * and a queue size of 10.
	 *
	 * @param executorService The executor service.
	 * @param shutdownOnClose Call {@link ExecutorService#shutdown()} when {@link RestClient#close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder executorService(ExecutorService executorService, boolean shutdownOnClose) {
		this.executorService = executorService;
		this.executorServiceShutdownOnClose = shutdownOnClose;
		return this;
	}


	//--------------------------------------------------------------------------------
	// HTTP headers
	//--------------------------------------------------------------------------------

	/**
	 * Specifies a request header property to add to all requests created by this client.
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder header(String name, Object value) {
		this.headers.put(name, value == null ? null : value.toString());
		return this;
	}

	/**
	 * Sets the value for the <code>Accept</code> request header.
	 * <p>
	 * This overrides the media type specified on the parser, but is overridden by calling <code>header(<js>"Accept"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder accept(Object value) {
		return header("Accept", value);
	}

	/**
	 * Sets the value for the <code>Accept-Charset</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Charset"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder acceptCharset(Object value) {
		return header("Accept-Charset", value);
	}

	/**
	 * Sets the value for the <code>Accept-Encoding</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Encoding"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder acceptEncoding(Object value) {
		return header("Accept-Encoding", value);
	}

	/**
	 * Sets the value for the <code>Accept-Language</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Language"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder acceptLanguage(Object value) {
		return header("Accept-Language", value);
	}

	/**
	 * Sets the value for the <code>Authorization</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder authorization(Object value) {
		return header("Authorization", value);
	}

	/**
	 * Sets the value for the <code>Cache-Control</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Cache-Control"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder cacheControl(Object value) {
		return header("Cache-Control", value);
	}

	/**
	 * Sets the value for the <code>Connection</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Connection"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder connection(Object value) {
		return header("Connection", value);
	}

	/**
	 * Sets the value for the <code>Content-Length</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Content-Length"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder contentLength(Object value) {
		return header("Content-Length", value);
	}

	/**
	 * Sets the value for the <code>Content-Type</code> request header.
	 * <p>
	 * This overrides the media type specified on the serializer, but is overridden by calling <code>header(<js>"Content-Type"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder contentType(Object value) {
		return header("Content-Type", value);
	}

	/**
	 * Sets the value for the <code>Date</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Date"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder date(Object value) {
		return header("Date", value);
	}

	/**
	 * Sets the value for the <code>Expect</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Expect"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder expect(Object value) {
		return header("Expect", value);
	}

	/**
	 * Sets the value for the <code>Forwarded</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Forwarded"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder forwarded(Object value) {
		return header("Forwarded", value);
	}

	/**
	 * Sets the value for the <code>From</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"From"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder from(Object value) {
		return header("From", value);
	}

	/**
	 * Sets the value for the <code>Host</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Host"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder host(Object value) {
		return header("Host", value);
	}

	/**
	 * Sets the value for the <code>If-Match</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifMatch(Object value) {
		return header("If-Match", value);
	}

	/**
	 * Sets the value for the <code>If-Modified-Since</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Modified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifModifiedSince(Object value) {
		return header("If-Modified-Since", value);
	}

	/**
	 * Sets the value for the <code>If-None-Match</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-None-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifNoneMatch(Object value) {
		return header("If-None-Match", value);
	}

	/**
	 * Sets the value for the <code>If-Range</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifRange(Object value) {
		return header("If-Range", value);
	}

	/**
	 * Sets the value for the <code>If-Unmodified-Since</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Unmodified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifUnmodifiedSince(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <code>Max-Forwards</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Max-Forwards"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder maxForwards(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <code>Origin</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Origin"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder origin(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <code>Pragma</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Pragma"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder pragma(Object value) {
		return header("Pragma", value);
	}

	/**
	 * Sets the value for the <code>Proxy-Authorization</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Proxy-Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder proxyAuthorization(Object value) {
		return header("Proxy-Authorization", value);
	}

	/**
	 * Sets the value for the <code>Range</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder range(Object value) {
		return header("Range", value);
	}

	/**
	 * Sets the value for the <code>Referer</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Referer"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder referer(Object value) {
		return header("Referer", value);
	}

	/**
	 * Sets the value for the <code>TE</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"TE"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder te(Object value) {
		return header("TE", value);
	}

	/**
	 * Sets the value for the <code>User-Agent</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"User-Agent"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder userAgent(Object value) {
		return header("User-Agent", value);
	}

	/**
	 * Sets the value for the <code>Upgrade</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Upgrade"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder upgrade(Object value) {
		return header("Upgrade", value);
	}

	/**
	 * Sets the value for the <code>Via</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Via"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder via(Object value) {
		return header("Via", value);
	}

	/**
	 * Sets the value for the <code>Warning</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Warning"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder warning(Object value) {
		return header("Warning", value);
	}


	//--------------------------------------------------------------------------------
	// CoreObject properties
	//--------------------------------------------------------------------------------

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_maxDepth} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_maxDepth
	 */
	public RestClientBuilder maxDepth(int value) {
		return property(SERIALIZER_maxDepth, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_initialDepth} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_initialDepth
	 */
	public RestClientBuilder initialDepth(int value) {
		return property(SERIALIZER_initialDepth, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_detectRecursions} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_detectRecursions
	 */
	public RestClientBuilder detectRecursions(boolean value) {
		return property(SERIALIZER_detectRecursions, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_ignoreRecursions} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_ignoreRecursions
	 */
	public RestClientBuilder ignoreRecursions(boolean value) {
		return property(SERIALIZER_ignoreRecursions, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_useWhitespace} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_useWhitespace
	 */
	public RestClientBuilder useWhitespace(boolean value) {
		return property(SERIALIZER_useWhitespace, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_maxIndent} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_maxIndent
	 */
	public RestClientBuilder maxIndent(boolean value) {
		return property(SERIALIZER_maxIndent, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_addBeanTypeProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_addBeanTypeProperties
	 */
	public RestClientBuilder addBeanTypeProperties(boolean value) {
		return property(SERIALIZER_addBeanTypeProperties, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_quoteChar} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_quoteChar
	 */
	public RestClientBuilder quoteChar(char value) {
		return property(SERIALIZER_quoteChar, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_trimNullProperties} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_trimNullProperties
	 */
	public RestClientBuilder trimNullProperties(boolean value) {
		return property(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_trimEmptyCollections} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_trimEmptyCollections
	 */
	public RestClientBuilder trimEmptyCollections(boolean value) {
		return property(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_trimEmptyMaps} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_trimEmptyMaps
	 */
	public RestClientBuilder trimEmptyMaps(boolean value) {
		return property(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_trimStrings} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_trimStrings
	 */
	public RestClientBuilder trimStrings(boolean value) {
		return property(SERIALIZER_trimStrings, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_uriContext} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_uriContext
	 */
	public RestClientBuilder uriContext(UriContext value) {
		return property(SERIALIZER_uriContext, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_uriResolution} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_uriResolution
	 */
	public RestClientBuilder uriResolution(UriResolution value) {
		return property(SERIALIZER_uriResolution, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_uriRelativity} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_uriRelativity
	 */
	public RestClientBuilder uriRelativity(UriRelativity value) {
		return property(SERIALIZER_uriRelativity, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_sortCollections} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_sortCollections
	 */
	public RestClientBuilder sortCollections(boolean value) {
		return property(SERIALIZER_sortCollections, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_sortMaps} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_sortMaps
	 */
	public RestClientBuilder sortMaps(boolean value) {
		return property(SERIALIZER_sortMaps, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_abridged} property on all serializers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_abridged
	 */
	public RestClientBuilder abridged(boolean value) {
		return property(SERIALIZER_abridged, value);
	}

	/**
	 * Sets the {@link SerializerContext#SERIALIZER_listener} and {@link ParserContext#PARSER_listener} property on all
	 * 	serializers and parsers in this group.
	 *
	 * @param sl The new serializer listener.
	 * @param pl The new parser listener.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_abridged
	 */
	public RestClientBuilder listeners(Class<? extends SerializerListener> sl, Class<? extends ParserListener> pl) {
		property(SERIALIZER_listener, sl);
		property(PARSER_listener, pl);
		return this;
	}

	/**
	 * Sets the {@link ParserContext#PARSER_trimStrings} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see ParserContext#PARSER_trimStrings
	 */
	public RestClientBuilder trimStringsP(boolean value) {
		return property(PARSER_trimStrings, value);
	}

	/**
	 * Sets the {@link ParserContext#PARSER_strict} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see ParserContext#PARSER_strict
	 */
	public RestClientBuilder strict(boolean value) {
		return property(PARSER_strict, value);
	}

	/**
	 * Sets the {@link ParserContext#PARSER_inputStreamCharset} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see ParserContext#PARSER_inputStreamCharset
	 */
	public RestClientBuilder inputStreamCharset(String value) {
		return property(PARSER_inputStreamCharset, value);
	}

	/**
	 * Sets the {@link ParserContext#PARSER_fileCharset} property on all parsers in this group.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see ParserContext#PARSER_fileCharset
	 */
	public RestClientBuilder fileCharset(String value) {
		return property(PARSER_fileCharset, value);
	}

	/**
	 * When called, <code>No-Trace: true</code> is added to requests.
	 * <p>
	 * This gives the opportunity for the servlet to not log errors on invalid requests.
	 * This is useful for testing purposes when you don't want your log file to show lots
	 * of errors that are simply the results of testing.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder noTrace() {
		return header("No-Trace", true);
	}

	/**
	 * Sets the {@link UonSerializerContext#UON_paramFormat} property on the URL-encoding serializers in this group.
	 * <p>
	 * This overrides the behavior of the URL-encoding serializer to quote and escape characters
	 * in query names and values that may be confused for UON notation (e.g. <js>"'(foo=123)'"</js>, <js>"'@(1,2,3)'"</js>).
	 * <p>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see UonSerializerContext#UON_paramFormat
	 */
	public RestClientBuilder paramFormat(String value) {
		super.property(UonSerializerContext.UON_paramFormat, value);
		return this;
	}

	/**
	 * Shortcut for calling <code>paramFormat(<js>"PLAINTEXT"</js>)</code>.
	 * <p>
	 * The default behavior is to serialize part values (query parameters, form data, headers, path variables) in UON notation.
	 * Calling this method forces plain-text to be used instead.
	 * <p>
	 * Specifially, UON notation has the following effects:
	 * <ul>
	 * 	<li>Boolean strings (<js>"true"</js>/<js>"false"</js>) and numeric values (<js>"123"</js>) will be
	 * 			quoted (<js>"'true'"</js>, <js>"'false'"</js>, <js>"'123'"</js>.
	 * 		<br>This allows them to be differentiated from actual boolean and numeric values.
	 * 	<li>String such as <js>"(foo='bar')"</js> that mimic UON structures will be quoted and escaped to
	 * 		<js>"'(foo=bar~'baz~')'"</js>.
	 * </ul>
	 * <p>
	 * The downside to using plain text part serialization is that you cannot serialize arbitrary POJOs.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder plainTextParts() {
		super.property(UonSerializerContext.UON_paramFormat, "PLAINTEXT");
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder notBeanPackages(Collection<String> values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setNotBeanPackages(String...values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setNotBeanPackages(Collection<String> values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removeNotBeanPackages(String...values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removeNotBeanPackages(Collection<String> values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder notBeanClasses(Collection<Class<?>> values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setNotBeanClasses(Class<?>...values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setNotBeanClasses(Collection<Class<?>> values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removeNotBeanClasses(Class<?>...values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beanFilters(Collection<Class<?>> values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setBeanFilters(Class<?>...values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setBeanFilters(Collection<Class<?>> values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removeBeanFilters(Class<?>...values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removeBeanFilters(Collection<Class<?>> values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder pojoSwaps(Collection<Class<?>> values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setPojoSwaps(Class<?>...values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setPojoSwaps(Collection<Class<?>> values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removePojoSwaps(Class<?>...values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removePojoSwaps(Collection<Class<?>> values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public <T> RestClientBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder includeProperties(Map<String,String> values) {
		super.includeProperties(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder includeProperties(String beanClassName, String properties) {
		super.includeProperties(beanClassName, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder includeProperties(Class<?> beanClass, String properties) {
		super.includeProperties(beanClass, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder excludeProperties(Map<String,String> values) {
		super.excludeProperties(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder excludeProperties(String beanClassName, String properties) {
		super.excludeProperties(beanClassName, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder excludeProperties(Class<?> beanClass, String properties) {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beanDictionary(Collection<Class<?>> values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setBeanDictionary(Class<?>...values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder setBeanDictionary(Collection<Class<?>> values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removeFromBeanDictionary(Class<?>...values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder defaultParser(Class<?> value) {
		super.defaultParser(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder debug() {
		super.debug();
		this.debug = true;
		header("Debug", true);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder property(String name, Object value) {
		super.property(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder properties(Map<String,Object> properties) {
		super.properties(properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder addToProperty(String name, Object value) {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder putToProperty(String name, Object key, Object value) {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder putToProperty(String name, Object value) {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder removeFromProperty(String name, Object value) {
		super.removeFromProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder classLoader(ClassLoader classLoader) {
		super.classLoader(classLoader);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RestClientBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}


	//------------------------------------------------------------------------------------------------
	// Passthrough methods for HttpClientBuilder.
	//------------------------------------------------------------------------------------------------

	/**
	 * @param redirectStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRedirectStrategy(RedirectStrategy)
	 */
	public RestClientBuilder setRedirectStrategy(RedirectStrategy redirectStrategy) {
		httpClientBuilder.setRedirectStrategy(redirectStrategy);
		return this;
	}

	/**
	 * @param cookieSpecRegistry
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieSpecRegistry(Lookup)
	 */
	public RestClientBuilder setDefaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
		httpClientBuilder.setDefaultCookieSpecRegistry(cookieSpecRegistry);
		return this;
	}

	/**
	 * @param requestExec
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRequestExecutor(HttpRequestExecutor)
	 */
	public RestClientBuilder setRequestExecutor(HttpRequestExecutor requestExec) {
		httpClientBuilder.setRequestExecutor(requestExec);
		return this;
	}

	/**
	 * @param hostnameVerifier
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLHostnameVerifier(HostnameVerifier)
	 */
	public RestClientBuilder setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
		httpClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
		return this;
	}

	/**
	 * @param publicSuffixMatcher
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setPublicSuffixMatcher(PublicSuffixMatcher)
	 */
	public RestClientBuilder setPublicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
		httpClientBuilder.setPublicSuffixMatcher(publicSuffixMatcher);
		return this;
	}

	/**
	 * @param sslContext
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLContext(SSLContext)
	 */
	public RestClientBuilder setSSLContext(SSLContext sslContext) {
		httpClientBuilder.setSSLContext(sslContext);
		return this;
	}

	/**
	 * @param sslSocketFactory
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLSocketFactory(LayeredConnectionSocketFactory)
	 */
	public RestClientBuilder setSSLSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
		httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
		return this;
	}

	/**
	 * @param maxConnTotal
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnTotal(int)
	 */
	public RestClientBuilder setMaxConnTotal(int maxConnTotal) {
		httpClientBuilder.setMaxConnTotal(maxConnTotal);
		return this;
	}

	/**
	 * @param maxConnPerRoute
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnPerRoute(int)
	 */
	public RestClientBuilder setMaxConnPerRoute(int maxConnPerRoute) {
		httpClientBuilder.setMaxConnPerRoute(maxConnPerRoute);
		return this;
	}

	/**
	 * @param config
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultSocketConfig(SocketConfig)
	 */
	public RestClientBuilder setDefaultSocketConfig(SocketConfig config) {
		httpClientBuilder.setDefaultSocketConfig(config);
		return this;
	}

	/**
	 * @param config
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultConnectionConfig(ConnectionConfig)
	 */
	public RestClientBuilder setDefaultConnectionConfig(ConnectionConfig config) {
		httpClientBuilder.setDefaultConnectionConfig(config);
		return this;
	}

	/**
	 * @param connTimeToLive
	 * @param connTimeToLiveTimeUnit
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionTimeToLive(long,TimeUnit)
	 */
	public RestClientBuilder setConnectionTimeToLive(long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
		httpClientBuilder.setConnectionTimeToLive(connTimeToLive, connTimeToLiveTimeUnit);
		return this;
	}

	/**
	 * @param connManager
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManager(HttpClientConnectionManager)
	 */
	public RestClientBuilder setConnectionManager(HttpClientConnectionManager connManager) {
		this.httpClientConnectionManager = connManager;
		httpClientBuilder.setConnectionManager(connManager);
		return this;
	}

	/**
	 * @param shared
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManagerShared(boolean)
	 */
	public RestClientBuilder setConnectionManagerShared(boolean shared) {
		httpClientBuilder.setConnectionManagerShared(shared);
		return this;
	}

	/**
	 * @param reuseStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionReuseStrategy(ConnectionReuseStrategy)
	 */
	public RestClientBuilder setConnectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
		httpClientBuilder.setConnectionReuseStrategy(reuseStrategy);
		return this;
	}

	/**
	 * @param keepAliveStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setKeepAliveStrategy(ConnectionKeepAliveStrategy)
	 */
	public RestClientBuilder setKeepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
		httpClientBuilder.setKeepAliveStrategy(keepAliveStrategy);
		return this;
	}

	/**
	 * @param targetAuthStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setTargetAuthenticationStrategy(AuthenticationStrategy)
	 */
	public RestClientBuilder setTargetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
		httpClientBuilder.setTargetAuthenticationStrategy(targetAuthStrategy);
		return this;
	}

	/**
	 * @param proxyAuthStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxyAuthenticationStrategy(AuthenticationStrategy)
	 */
	public RestClientBuilder setProxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
		httpClientBuilder.setProxyAuthenticationStrategy(proxyAuthStrategy);
		return this;
	}

	/**
	 * @param userTokenHandler
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserTokenHandler(UserTokenHandler)
	 */
	public RestClientBuilder setUserTokenHandler(UserTokenHandler userTokenHandler) {
		httpClientBuilder.setUserTokenHandler(userTokenHandler);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableConnectionState()
	 */
	public RestClientBuilder disableConnectionState() {
		httpClientBuilder.disableConnectionState();
		return this;
	}

	/**
	 * @param schemePortResolver
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSchemePortResolver(SchemePortResolver)
	 */
	public RestClientBuilder setSchemePortResolver(SchemePortResolver schemePortResolver) {
		httpClientBuilder.setSchemePortResolver(schemePortResolver);
		return this;
	}

	/**
	 * @param userAgent
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserAgent(String)
	 */
	public RestClientBuilder setUserAgent(String userAgent) {
		httpClientBuilder.setUserAgent(userAgent);
		return this;
	}

	/**
	 * @param defaultHeaders
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultHeaders(Collection)
	 */
	public RestClientBuilder setDefaultHeaders(Collection<? extends Header> defaultHeaders) {
		httpClientBuilder.setDefaultHeaders(defaultHeaders);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpResponseInterceptor)
	 */
	public RestClientBuilder addInterceptorFirst(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpResponseInterceptor)
	 */
	public RestClientBuilder addInterceptorLast(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpRequestInterceptor)
	 */
	public RestClientBuilder addInterceptorFirst(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpRequestInterceptor)
	 */
	public RestClientBuilder addInterceptorLast(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableCookieManagement()
	 */
	public RestClientBuilder disableCookieManagement() {
		httpClientBuilder.disableCookieManagement();
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableContentCompression()
	 */
	public RestClientBuilder disableContentCompression() {
		httpClientBuilder.disableContentCompression();
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAuthCaching()
	 */
	public RestClientBuilder disableAuthCaching() {
		httpClientBuilder.disableAuthCaching();
		return this;
	}

	/**
	 * @param httpprocessor
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setHttpProcessor(HttpProcessor)
	 */
	public RestClientBuilder setHttpProcessor(HttpProcessor httpprocessor) {
		httpClientBuilder.setHttpProcessor(httpprocessor);
		return this;
	}

	/**
	 * @param retryHandler
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRetryHandler(HttpRequestRetryHandler)
	 */
	public RestClientBuilder setRetryHandler(HttpRequestRetryHandler retryHandler) {
		httpClientBuilder.setRetryHandler(retryHandler);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAutomaticRetries()
	 */
	public RestClientBuilder disableAutomaticRetries() {
		httpClientBuilder.disableAutomaticRetries();
		return this;
	}

	/**
	 * @param proxy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxy(HttpHost)
	 */
	public RestClientBuilder setProxy(HttpHost proxy) {
		httpClientBuilder.setProxy(proxy);
		return this;
	}

	/**
	 * @param routePlanner
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRoutePlanner(HttpRoutePlanner)
	 */
	public RestClientBuilder setRoutePlanner(HttpRoutePlanner routePlanner) {
		httpClientBuilder.setRoutePlanner(routePlanner);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableRedirectHandling()
	 */
	public RestClientBuilder disableRedirectHandling() {
		httpClientBuilder.disableRedirectHandling();
		return this;
	}

	/**
	 * @param connectionBackoffStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionBackoffStrategy(ConnectionBackoffStrategy)
	 */
	public RestClientBuilder setConnectionBackoffStrategy(ConnectionBackoffStrategy connectionBackoffStrategy) {
		httpClientBuilder.setConnectionBackoffStrategy(connectionBackoffStrategy);
		return this;
	}

	/**
	 * @param backoffManager
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setBackoffManager(BackoffManager)
	 */
	public RestClientBuilder setBackoffManager(BackoffManager backoffManager) {
		httpClientBuilder.setBackoffManager(backoffManager);
		return this;
	}

	/**
	 * @param serviceUnavailStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setServiceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy)
	 */
	public RestClientBuilder setServiceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
		httpClientBuilder.setServiceUnavailableRetryStrategy(serviceUnavailStrategy);
		return this;
	}

	/**
	 * @param cookieStore
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieStore(CookieStore)
	 */
	public RestClientBuilder setDefaultCookieStore(CookieStore cookieStore) {
		httpClientBuilder.setDefaultCookieStore(cookieStore);
		return this;
	}

	/**
	 * @param credentialsProvider
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCredentialsProvider(CredentialsProvider)
	 */
	public RestClientBuilder setDefaultCredentialsProvider(CredentialsProvider credentialsProvider) {
		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		return this;
	}

	/**
	 * @param authSchemeRegistry
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultAuthSchemeRegistry(Lookup)
	 */
	public RestClientBuilder setDefaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
		httpClientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
		return this;
	}

	/**
	 * @param contentDecoderMap
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setContentDecoderRegistry(Map)
	 */
	public RestClientBuilder setContentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap) {
		httpClientBuilder.setContentDecoderRegistry(contentDecoderMap);
		return this;
	}

	/**
	 * @param config
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultRequestConfig(RequestConfig)
	 */
	public RestClientBuilder setDefaultRequestConfig(RequestConfig config) {
		httpClientBuilder.setDefaultRequestConfig(config);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#useSystemProperties()
	 */
	public RestClientBuilder useSystemProperties() {
		httpClientBuilder.useSystemProperties();
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictExpiredConnections()
	 */
	public RestClientBuilder evictExpiredConnections() {
		httpClientBuilder.evictExpiredConnections();
		return this;
	}

	/**
	 * @param maxIdleTime
	 * @param maxIdleTimeUnit
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictIdleConnections(long,TimeUnit)
	 */
	public RestClientBuilder evictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
		httpClientBuilder.evictIdleConnections(maxIdleTime, maxIdleTimeUnit);
		return this;
	}
}
