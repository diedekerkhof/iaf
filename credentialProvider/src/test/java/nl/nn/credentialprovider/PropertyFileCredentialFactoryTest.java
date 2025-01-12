package nl.nn.credentialprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class PropertyFileCredentialFactoryTest {

	public String PROPERTIES_FILE="/credentials-unencrypted.txt";
	
	private PropertyFileCredentialFactory credentialFactory;
	
	@Before
	public void setup() throws IOException {
		String propertiesUrl = this.getClass().getResource(PROPERTIES_FILE).toExternalForm();
		String propertiesFile =  Paths.get(propertiesUrl.substring(propertiesUrl.indexOf(":/")+2)).toString();
		assumeTrue(Files.exists(Paths.get(propertiesFile)));

		System.setProperty("credentialFactory.map.properties", propertiesFile);
		
		credentialFactory = new PropertyFileCredentialFactory();
		credentialFactory.initialize();
	}
	

	@Test
	public void testNoAlias() {
		
		String alias = null;
		String username = "fakeUsername";
		String password = "fakePassword";
		
		ICredentials mc = credentialFactory.getCredentials(alias, username, password);
		
		assertEquals(username, mc.getUsername());
		assertEquals(password, mc.getPassword());
	}

	@Test
	public void testPlainAlias() {
		
		String alias = "straight";
		String defaultUsername = "fakeDefaultUsername";
		String defaultPassword = "fakeDefaultPassword";
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";
		
		ICredentials mc = credentialFactory.getCredentials(alias, defaultUsername, defaultPassword);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testUnknownAlias() {
		
		String alias = "unknown";
		String defaultUsername = "fakeDefaultUsername";
		String defaultPassword = "fakeDefaultPassword";
		String expectedUsername = defaultUsername;
		String expectedPassword = defaultPassword;
		
		ICredentials mc = credentialFactory.getCredentials(alias, defaultUsername, defaultPassword);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}
	
	@Test
	public void testAliasWithoutUsername() {
		
		String alias = "noUsername";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = username;
		String expectedPassword = "password from alias";
		
		ICredentials mc = credentialFactory.getCredentials(alias, username, password);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testPlainCredential() {
		
		String alias = "singleValue";
		String username = null;
		String password = "fakePassword";
		String expectedUsername = null;
		String expectedPassword = "Plain Credential";
		
		ICredentials mc = credentialFactory.getCredentials(alias, username, password);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}
	
	
}
