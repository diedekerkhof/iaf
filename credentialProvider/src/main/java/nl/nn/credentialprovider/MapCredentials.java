/*
   Copyright 2021 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.credentialprovider;

import java.util.Map;

import nl.nn.credentialprovider.util.Misc;

public class MapCredentials extends Credentials {

	private String usernameSuffix;
	private String passwordSuffix;

	private Map<String,String> aliases;


	public MapCredentials(String alias, String defaultUsername, String defaultPassword, Map<String,String> aliases) {
		this(alias, defaultUsername, defaultPassword, null, null, aliases);
	}

	public MapCredentials(String alias, String defaultUsername, String defaultPassword, String usernameSuffix, String passwordSuffix, Map<String,String> aliases) {
		super(alias, defaultUsername, defaultPassword);
		this.aliases = aliases;
		this.usernameSuffix = Misc.isNotEmpty(usernameSuffix) ? usernameSuffix : MapCredentialFactory.USERNAME_SUFFIX_DEFAULT;
		this.passwordSuffix = Misc.isNotEmpty(passwordSuffix) ? passwordSuffix : MapCredentialFactory.PASSWORD_SUFFIX_DEFAULT;
	}

	@Override
	protected void getCredentialsFromAlias() {
		if (Misc.isNotEmpty(getAlias()) && aliases!=null) {
			String usernameKey = getAlias()+usernameSuffix;
			String passwordKey = getAlias()+passwordSuffix;
			boolean foundOne = false;
			if (aliases.containsKey(usernameKey)) {
				foundOne = true;
				setUsername(aliases.get(usernameKey));
			}
			if (aliases.containsKey(passwordKey)) {
				foundOne = true;
				setPassword(aliases.get(passwordKey));
			}
			if (!foundOne && aliases.containsKey(getAlias())) {
				setPassword(aliases.get(getAlias()));
			}
		}
	}

}