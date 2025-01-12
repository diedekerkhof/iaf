package nl.nn.credentialprovider;

import java.util.HashMap;

public class MockCredentialFactory extends HashMap<String,Credentials> implements ICredentialFactory {

	@Override
	public boolean hasCredentials(String alias) {
		return containsKey(alias);
	}

	@Override
	public Credentials getCredentials(String alias, String defaultUsername, String defaultPassword) {
		Credentials result = new Credentials(alias, defaultUsername, defaultPassword);
		Credentials entry = get(alias);
		if (entry!=null) {
			if (entry.getUsername()!=null && !entry.getUsername().isEmpty()) result.setUsername(entry.getUsername());
			if (entry.getPassword()!=null && !entry.getPassword().isEmpty()) result.setPassword(entry.getPassword());
		}
		return result;
	}

	public void add(String alias, String username, String password) {
		put(alias, new Credentials(alias, username, password));
	}
}
