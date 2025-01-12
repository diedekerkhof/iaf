/*
   Copyright 2018, 2020 Nationale-Nederlanden, 2020 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.pipes;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Pipe that hashes the input.
 * 
 * 
 * @author	Niels Meijer
 */
public class HashPipe extends FixedForwardPipe {

	private String algorithm = "HmacSHA256";
	private String charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private String secret = null;
	private String authAlias = null;
	private String binaryToTextEncoding = "Base64";

	protected List<String> algorithms = Arrays.asList("HmacMD5", "HmacSHA1", "HmacSHA256", "HmacSHA384", "HmacSHA512");
	protected List<String> binaryToTextEncodings = Arrays.asList("Base64", "Hex");

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (!algorithms.contains(getAlgorithm())) {
			throw new ConfigurationException("illegal value for algorithm [" + getAlgorithm() + "], must be one of " + algorithms.toString());
		}
		
		if (!binaryToTextEncodings.contains(getBinaryToTextEncoding())) {
			throw new ConfigurationException("illegal value for binary to text method [" + getBinaryToTextEncoding() + "], must be one of " + binaryToTextEncodings.toString());
		}
	}

	@Override
	public PipeRunResult doPipe (Message message, PipeLineSession session) throws PipeRunException {
		String authAlias = getAuthAlias();
		String secret = getSecret();
		try {
			ParameterList parameterList = getParameterList();
			ParameterValueList pvl = parameterList==null ? null : parameterList.getValues(message, session);
			if(pvl != null) {
				String authAliasParamValue = (String)pvl.getValue("authAlias");
				if (StringUtils.isNotEmpty(authAliasParamValue)) {
					authAlias = authAliasParamValue;
				}
				String secretParamValue = (String)pvl.getValue("secret");
				if (StringUtils.isNotEmpty(secretParamValue)) {
					secret = secretParamValue;
				}
			}
		}
		catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + "exception extracting authAlias", e);
		}

		CredentialFactory accessTokenCf = new CredentialFactory(authAlias, "", secret);
		String cfSecret = accessTokenCf.getPassword();

		if(cfSecret == null || cfSecret.isEmpty())
			throw new PipeRunException(this, getLogPrefix(session) + "empty secret, unable to hash");

		try {
			Mac mac = Mac.getInstance(getAlgorithm());

			SecretKeySpec secretkey = new SecretKeySpec(cfSecret.getBytes(getCharset()), "algorithm");
			mac.init(secretkey);

			try (InputStream inputStream = message.asInputStream()) {
				byte[] byteArray = new byte[1024];
				int readLength;
				while ((readLength = inputStream.read(byteArray)) != -1) {
					mac.update(byteArray, 0, readLength);
				}
			}
			
			String hash = "";
			if ("base64".equalsIgnoreCase(getBinaryToTextEncoding())) {
				hash = Base64.encodeBase64String(mac.doFinal());
			} else if ("hex".equalsIgnoreCase(getBinaryToTextEncoding())) {
				hash = Hex.encodeHexString(mac.doFinal());
			}else {
				// Should never happen, as a ConfigurationException is thrown during configuration if another method is tried
				throw new PipeRunException(this, getLogPrefix(session) + "error determining binaryToText method");
			}
			
			return new PipeRunResult(getSuccessForward(), hash);
		}
		catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + "error creating hash", e);
		}
	}

	@IbisDoc({"1", "Hashing algoritm to use, one of HmacMD5, HmacSHA1, HmacSHA256, HmacSHA384 or HmacSHA512", "hmacsha256"})
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
	public String getAlgorithm() {
		return algorithm;
	}

	@Deprecated
	@ConfigurationWarning("attribute encoding has been replaced with attribute charset, default has changed from ISO8859_1 to UTF-8")
	public void setEncoding(String encoding) {
		setCharset(encoding);
	}
	@IbisDoc({"2", "Character set to use for converting the secret from String to bytes", "UTF-8"})
	public void setCharset(String charset) {
		this.charset = charset;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"3","method to use for converting the hash from bytes to String, one of Base64 or Hex", "Base64"})
	public void setBinaryToTextEncoding(String binaryToTextEncoding) {
		this.binaryToTextEncoding = binaryToTextEncoding;
	}
	public String getBinaryToTextEncoding() {
		return binaryToTextEncoding;
	}

	@IbisDoc({"4", "The secret to hash with. Only used if no parameter secret is configured. The secret is only used when there is no authAlias specified, by attribute or parameter", ""})
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public String getSecret() {
		return secret;
	}


	@IbisDoc({"5","authAlias to retrieve the secret from (password field). Only used if no parameter authAlias is configured", ""})
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
	public String getAuthAlias() {
		return authAlias;
	}

}